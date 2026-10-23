package com.empmanage.cms.repository;

import com.empmanage.cms.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Employee> findByDepartment_NameIgnoreCase(String departmentName);

    List<Employee> findByDepartment_Id(Long departmentId);

    List<Employee> findByStatusIgnoreCase(String status);

    long countByStatusIgnoreCase(String status);

    long countByDepartment_Id(Long departmentId);

    long countByDepartment_NameIgnoreCase(String departmentName);

    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(e.employeeCode IS NOT NULL AND LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(e.department IS NOT NULL AND LOWER(e.department.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Employee> searchEmployees(@Param("query") String query);

    @Query("SELECT e FROM Employee e WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " (e.employeeCode IS NOT NULL AND LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :query, '%')))) AND " +
           "(:dept IS NULL OR :dept = '' OR :dept = 'ALL' OR (e.department IS NOT NULL AND (LOWER(e.department.name) = LOWER(:dept) OR LOWER(e.department.name) LIKE LOWER(CONCAT('%', :dept, '%'))))) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR LOWER(e.status) = LOWER(:status))")
    Page<Employee> findWithFilters(@Param("query") String query,
                                   @Param("dept") String dept,
                                   @Param("status") String status,
                                   Pageable pageable);

    List<Employee> findTop5ByOrderByJoiningDateDescIdDesc();
}
