package com.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dao.AttendanceDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.request.AttendanceDeleteRequestDto;
import com.inventory.dto.request.AttendanceRequestDto;
import com.inventory.dto.request.AttendanceSearchRequestDto;
import com.inventory.entity.Attendance;
import com.inventory.entity.Employee;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.AttendanceRepository;
import com.inventory.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final BigDecimal DEFAULT_REGULAR_HOURS = new BigDecimal("8.00");
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UtilityService utilityService;
    private final AttendanceDao attendanceDao;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> saveAttendance(AttendanceRequestDto request) {
        try {
            validateRequest(request);
            var currentUser = utilityService.getCurrentLoggedInUser();
            
            // Fetch all employees in one query
            Map<Long, Employee> employeeMap = employeeRepository.findAllById(request.getEmployeeIds())
                .stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
            
            List<Attendance> attendances = request.getEmployeeIds().stream()
                .map(employeeId -> {
                    Employee employee = employeeMap.get(employeeId);
                    if (employee == null) {
                        throw new ValidationException("Employee not found with ID: " + employeeId);
                    }
                    
                    return createAttendanceWithSalary(employee, request, currentUser);
                })
                .collect(Collectors.toList());
            
            attendanceRepository.saveAll(attendances);
            
            return ApiResponse.success("Attendance saved successfully", attendances.size());
        } catch (ValidationException e) {
            logger.error("Validation error: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save attendance", e);
            throw new ValidationException("Failed to save attendance: " + e.getMessage());
        }
    }

    private Attendance createAttendanceWithSalary(Employee employee, AttendanceRequestDto request, UserMaster currentUser) {
        // Calculate hours worked
        Duration duration = Duration.between(request.getStartDateTime(), request.getEndDateTime());
        BigDecimal hoursWorked = BigDecimal.valueOf(duration.toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        
        // Get regular hours (default to 8 if not set)
        BigDecimal regularHours = employee.getRegularHours() != null && employee.getRegularHours().compareTo(BigDecimal.ZERO) > 0 
            ? employee.getRegularHours() 
            : DEFAULT_REGULAR_HOURS;
        
        // Calculate overtime hours
        BigDecimal overtimeHours = hoursWorked.compareTo(regularHours) > 0 
            ? hoursWorked.subtract(regularHours)
            : BigDecimal.ZERO;
        
        // Calculate pays
        BigDecimal regularPay = calculateRegularPay(employee, regularHours);
        BigDecimal overtimePay = calculateOvertimePay(employee, overtimeHours);
        BigDecimal totalPay = regularPay.add(overtimePay);
        
        return Attendance.builder()
            .employee(employee)
            .startDateTime(request.getStartDateTime())
            .endDateTime(request.getEndDateTime())
            .regularHours(regularHours)
            .overtimeHours(overtimeHours)
            .regularPay(regularPay)
            .overtimePay(overtimePay)
            .totalPay(totalPay)
            .remarks(request.getRemarks())
            .client(currentUser.getClient())
            .createdBy(currentUser)
            .createdAt(OffsetDateTime.now())
            .build();
    }

    private BigDecimal calculateRegularPay(Employee employee, BigDecimal regularHours) {
        if (!"HOURLY".equalsIgnoreCase(employee.getWageType())) {
            return employee.getRegularPay() != null ? employee.getRegularPay() : BigDecimal.ZERO;
        }
        
        BigDecimal hourlyRate = employee.getRegularPay() != null ? 
            employee.getRegularPay() : BigDecimal.ZERO;
        
        return hourlyRate.multiply(regularHours)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOvertimePay(Employee employee, BigDecimal overtimeHours) {
        if (!"HOURLY".equalsIgnoreCase(employee.getWageType()) || 
            overtimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal overtimeRate = employee.getOvertimePay() != null ? 
            employee.getOvertimePay() : BigDecimal.ZERO;
        
        return overtimeRate.multiply(overtimeHours)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateRequest(AttendanceRequestDto request) {
        if (request.getEmployeeIds() == null || request.getEmployeeIds().isEmpty()) {
            throw new ValidationException("No employee IDs provided");
        }
        
        if (request.getStartDateTime() == null) {
            throw new ValidationException("Start date time is required");
        }
        
        if (request.getEndDateTime() == null) {
            throw new ValidationException("End date time is required");
        }
        
        if (request.getEndDateTime().isBefore(request.getStartDateTime())) {
            throw new ValidationException("End date time cannot be before start date time");
        }
    }

    public ApiResponse<?> getAttendanceByEmployee(AttendanceSearchRequestDto request) {
        try {
            if (request.getEmployeeId() == null) {
                throw new ValidationException("Employee ID is required");
            }
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> result = attendanceDao.getAttendanceByEmployee(
                request.getEmployeeId(),
                currentUser.getClient().getId(),
                request.getPage(),
                request.getSize(),
                request
            );
            
            return ApiResponse.success("Attendance records retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch attendance records: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> deleteAttendances(AttendanceDeleteRequestDto request) {
        try {
            if (request.getAttendanceIds() == null || request.getAttendanceIds().isEmpty()) {
                throw new ValidationException("No attendance IDs provided");
            }

            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            List<Attendance> attendances = attendanceRepository.findAllById(request.getAttendanceIds());

            if (attendances.size() != request.getAttendanceIds().size()) {
                throw new ValidationException("One or more attendance records not found");
            }
            
            for (Attendance attendance : attendances) {
                if (!attendance.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("You are not authorized to delete some of these records");
                }
            }
            
            attendanceRepository.deleteAllById(request.getAttendanceIds());
            
            return ApiResponse.success("Attendance records deleted successfully", request.getAttendanceIds().size());
        } catch (Exception e) {
            throw new ValidationException("Failed to delete attendance records: " + e.getMessage());
        }
    }
} 