package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.CoatingPriceDto;
import com.inventory.dto.CustomerDto;
import com.inventory.entity.Customer;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.dao.CustomerDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerDao customerDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(CustomerDto dto) {
        try {
            validateCustomer(dto);
            
//            Optional<Customer> existingCustomer = customerRepository.findByMobile(dto.getMobile().trim());
//            if (existingCustomer.isPresent()) {
//                throw new ValidationException("Customer with this mobile number already exists");
//            }

            Customer customer = new Customer();
            mapDtoToEntity(dto, customer);
            customer.setCreatedBy(utilityService.getCurrentLoggedInUser());
            customer.setClient(utilityService.getCurrentLoggedInUser().getClient());
            customer = customerRepository.save(customer);
            return ApiResponse.success("Customer created successfully", mapEntityToDto(customer));
        } catch (ValidationException e) {
            e.getMessage();
            throw e;
        } catch (Exception e) {
            e.getMessage();
            throw new ValidationException("Failed to create customer: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, CustomerDto dto) {
        try {
            validateCustomer(dto);
            
            Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Customer not found"));

            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this customer");
            }

//            Optional<Customer> existingCustomer = customerRepository.findByMobileAndIdNot(
//                dto.getMobile().trim(), customer.getId());
//            if (existingCustomer.isPresent()) {
//                throw new ValidationException("Customer with this mobile number already exists");
//            }

            mapDtoToEntity(dto, customer);
            customer.setUpdatedAt(OffsetDateTime.now());
            
            customer = customerRepository.save(customer);
            return ApiResponse.success("Customer updated successfully", mapEntityToDto(customer));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update customer: " + e.getMessage());
        }
    }

    public ApiResponse<?> delete(Long id) {
        try {
            Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Customer not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this customer");
            }
            customerRepository.delete(customer);
            return ApiResponse.success("Customer deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete customer: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchCustomers(CustomerDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = customerDao.searchCustomers(dto);
            return ApiResponse.success("Customers retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search customers: " + e.getMessage());
        }
    }

    public ApiResponse<List<Map<String, Object>>> getCustomers(CustomerDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            List<Map<String, Object>> customers = customerDao.getCustomers(dto);
            return ApiResponse.success("Customers retrieved successfully", customers);
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve customers: " + e.getMessage());
        }
    }

    public ApiResponse<?> getCustomer(Long id) {
        try {
            if (id == null) {
                throw new ValidationException("Customer ID is required");
            }
            
            Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Customer not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to view this customer");
            }
            
            return ApiResponse.success("Customer retrieved successfully", mapEntityToDto(customer));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve customer: " + e.getMessage());
        }
    }

    public ApiResponse<?> getCoatingPrice(CoatingPriceDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Customer ID is required");
            }
            
            Customer customer = customerRepository.findById(dto.getId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to view this customer");
            }

            CoatingPriceDto response = new CoatingPriceDto();
            response.setId(customer.getId());
            response.setCoatingUnitPrice(customer.getCoatingUnitPrice());
            
            return ApiResponse.success("Coating price retrieved successfully", response);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve coating price: " + e.getMessage());
        }
    }

    private void validateCustomer(CustomerDto dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new ValidationException("Name is required");
        }
        
//        if (!StringUtils.hasText(dto.getMobile())) {
//            throw new ValidationException("Mobile number is required");
//        }
//
//        if (dto.getMobile().length() < 10 || dto.getMobile().length() > 15) {
//            throw new ValidationException("Invalid mobile number");
//        }
    }

    private void mapDtoToEntity(CustomerDto dto, Customer customer) {
        customer.setName(dto.getName().trim());
        customer.setGst(dto.getGst() != null ? dto.getGst().trim() : null);
        customer.setAddress(dto.getAddress());
        customer.setMobile(dto.getMobile().trim());
        customer.setRemainingPaymentAmount(dto.getRemainingPaymentAmount());
        customer.setCoatingUnitPrice(dto.getCoatingUnitPrice());
        customer.setNextActionDate(dto.getNextActionDate());
        customer.setEmail(dto.getEmail());
        customer.setRemarks(dto.getRemarks());
        customer.setStatus(dto.getStatus() != null ? dto.getStatus() : "A");
    }

    private CustomerDto mapEntityToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setGst(customer.getGst());
        dto.setAddress(customer.getAddress());
        dto.setMobile(customer.getMobile());
        dto.setRemainingPaymentAmount(customer.getRemainingPaymentAmount());
        dto.setCoatingUnitPrice(customer.getCoatingUnitPrice());
        dto.setNextActionDate(customer.getNextActionDate());
        dto.setEmail(customer.getEmail());
        dto.setRemarks(customer.getRemarks());
        dto.setStatus(customer.getStatus());
        dto.setClientId(customer.getClient().getId());
        return dto;
    }
} 