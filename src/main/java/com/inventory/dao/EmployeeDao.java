package com.inventory.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.inventory.dto.EmployeeDto;
import com.inventory.exception.ValidationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class EmployeeDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchEmployees(EmployeeDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("SELECT COUNT(e.id) FROM employee e WHERE 1=1");
        appendSearchConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                e.id, e.name, e.mobile_number, e.email,
                e.address, e.designation, e.department,
                e.status, e.created_at
            FROM employee e
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, dto);
        sql.append("""
            ORDER BY e.%s %s
            LIMIT :pageSize OFFSET :offset
        """.formatted(dto.getSortBy(), dto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, dto);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto.getSize());
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, EmployeeDto dto) {
        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(e.name) LIKE LOWER(:search) OR e.mobile_number LIKE :search)");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        
        if (StringUtils.hasText(dto.getStatus())) {
            sql.append(" AND e.status = :status");
            params.put("status", dto.getStatus().trim());
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, EmployeeDto dto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> employees = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", row[0]);
            employee.put("name", row[1]);
            employee.put("mobileNumber", row[2]);
            employee.put("email", row[3]);
            employee.put("address", row[4]);
            employee.put("designation", row[5]);
            employee.put("department", row[6]);
            employee.put("status", row[7]);
            employee.put("createdAt", row[8]);
            employees.add(employee);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", employees);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }

    public Map<String, Object> getEmployeeDetail(Long employeeId) {
        String query = """
            SELECT 
                e.id,
                e.name,
                e.mobile_number,
                e.aadhar_number,
                e.email,
                e.address,
                e.designation,
                e.department,
                e.status,
                e.wage_type,
                e.regular_hours,
                e.start_time,
                e.regular_pay,
                e.overtime_pay
            FROM employee e
            WHERE e.id = :employeeId
        """;
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("employeeId", employeeId);
        
        Object[] result = (Object[]) nativeQuery.getSingleResult();
        
        if (result == null) {
            throw new ValidationException("Employee not found");
        }
        
        Map<String, Object> employee = new HashMap<>();
        int index = 0;
        employee.put("id", result[index++]);
        employee.put("name", result[index++]);
        employee.put("mobileNumber", result[index++]);
        employee.put("aadharNumber", result[index++]);
        employee.put("email", result[index++]);
        employee.put("address", result[index++]);
        employee.put("designation", result[index++]);
        employee.put("department", result[index++]);
        employee.put("status", result[index++]);
        employee.put("wageType", result[index++]);
        employee.put("regularHours", result[index++]);
        employee.put("startTime", result[index++]);
        employee.put("regularPay", result[index++]);
        employee.put("overtimePay", result[index++]);
        return employee;
    }

    public List<Map<String, Object>> getAllEmployees(Long clientId) {
        String sql = """
            SELECT 
                e.id,
                e.name
            FROM employee e
            WHERE e.status = 'A' AND e.client_id = :clientId
            ORDER BY e.name ASC
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("clientId", clientId);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> employees = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", row[0]);
            employee.put("name", row[1]);
            employees.add(employee);
        }
        
        return employees;
    }
} 