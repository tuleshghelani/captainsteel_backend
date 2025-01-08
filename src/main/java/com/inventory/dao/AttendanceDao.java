package com.inventory.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.inventory.dto.request.AttendanceSearchRequestDto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class AttendanceDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public Map<String, Object> getAttendanceByEmployee(Long employeeId, Long clientId, Integer page, Integer size, AttendanceSearchRequestDto request) {
        StringBuilder baseCondition = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        baseCondition.append(" WHERE a.employee_id = :employeeId AND a.client_id = :clientId");
        params.put("employeeId", employeeId);
        params.put("clientId", clientId);
        
        if (request.getStartDate() != null && request.getEndDate() != null) {
            baseCondition.append(" AND a.start_date_time >= :startDate AND a.start_date_time <= :endDate");
            params.put("startDate", request.getStartDate());
            params.put("endDate", request.getEndDate());
        }
        
        // Count Query
        String countSql = "SELECT COUNT(a.id) FROM attendance a" + baseCondition;
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, params);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();
        
        // Main Query
        String sql = """
            SELECT 
                a.id, a.start_date_time, a.end_date_time, 
                a.remarks, a.created_at,
                e.id as employee_id, e.name as employee_name,
                u.id as created_by_id, u.first_name as created_by_first_name, u.last_name as created_by_last_name
            FROM attendance a
            JOIN employee e ON a.employee_id = e.id
            JOIN user_master u ON a.created_by = u.id
        """ + baseCondition + 
        " ORDER BY a.start_date_time DESC LIMIT :pageSize OFFSET :offset";
        
        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, params);
        query.setParameter("pageSize", size);
        query.setParameter("offset", page * size);
        
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, size);
    }
    
    private void setQueryParameters(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }
    
    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> attendances = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> attendance = new HashMap<>();
            attendance.put("id", row[0]);
            attendance.put("startDateTime", row[1]);
            attendance.put("endDateTime", row[2]);
            attendance.put("remarks", row[3]);
            attendance.put("createdAt", row[4]);
            
            attendance.put("employee", Map.of(
                "id", row[5],
                "name", row[6]
            ));
            
            String createdByName = String.format("%s %s", 
                row[8] != null ? row[8] : "",
                row[9] != null ? row[9] : "").trim();
            
            attendance.put("createdBy", Map.of(
                "id", row[7],
                "name", createdByName
            ));
            
            attendances.add(attendance);
        }
        
        return Map.of(
            "content", attendances,
            "totalElements", totalRecords,
            "totalPages", (totalRecords + pageSize - 1) / pageSize
        );
    }
} 