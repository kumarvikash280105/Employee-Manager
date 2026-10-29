package com.empmanage.cms.repository;

import com.empmanage.cms.entity.Task;
import com.empmanage.cms.entity.TaskPriority;
import com.empmanage.cms.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedToEmailIgnoreCaseOrderByDueDateAsc(String email);

    List<Task> findByAssignedToEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<Task> findAllByOrderByCreatedAtDesc();

    List<Task> findByAssignedToDepartmentIgnoreCaseOrderByDueDateAsc(String department);

    long countByStatus(TaskStatus status);

    long countByAssignedToEmailIgnoreCase(String email);

    long countByAssignedToEmailIgnoreCaseAndStatus(String email, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < CURRENT_DATE " +
           "AND t.status NOT IN (com.empmanage.cms.entity.TaskStatus.COMPLETED, com.empmanage.cms.entity.TaskStatus.CANCELLED)")
    long countOverdueTasks();

    @Query("SELECT COUNT(t) FROM Task t WHERE LOWER(t.assignedToEmail) = LOWER(:email) " +
           "AND t.dueDate IS NOT NULL AND t.dueDate < CURRENT_DATE " +
           "AND t.status NOT IN (com.empmanage.cms.entity.TaskStatus.COMPLETED, com.empmanage.cms.entity.TaskStatus.CANCELLED)")
    long countUserOverdueTasks(@Param("email") String email);

    @Query("SELECT t FROM Task t WHERE " +
           "(:assigneeEmail IS NULL OR :assigneeEmail = '' OR LOWER(t.assignedToEmail) = LOWER(:assigneeEmail)) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(t.assignedToDepartment) = LOWER(:department)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.assignedToName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.assignedToEmail) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findTasksWithFilter(@Param("assigneeEmail") String assigneeEmail,
                                   @Param("status") TaskStatus status,
                                   @Param("priority") TaskPriority priority,
                                   @Param("department") String department,
                                   @Param("query") String query);

    @Query("SELECT t FROM Task t WHERE " +
           "LOWER(t.assignedToEmail) = LOWER(:email) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.dueDate ASC NULLS LAST, t.createdAt DESC")
    List<Task> findMyTasksWithFilter(@Param("email") String email,
                                     @Param("status") TaskStatus status,
                                     @Param("priority") TaskPriority priority,
                                     @Param("query") String query);

    @Query("SELECT DISTINCT t.assignedToDepartment FROM Task t WHERE t.assignedToDepartment IS NOT NULL")
    List<String> findDistinctDepartments();
}
