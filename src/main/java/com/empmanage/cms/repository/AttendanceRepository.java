package com.empmanage.cms.repository;

import com.empmanage.cms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByUserEmailIgnoreCaseOrderByTimestampDesc(String userEmail);
    Optional<Attendance> findByUserEmailIgnoreCaseAndDateKey(String userEmail, String dateKey);
    List<Attendance> findByDateKey(String dateKey);
    List<Attendance> findAllByOrderByTimestampDesc();
    long countByDateKeyAndStatusIgnoreCase(String dateKey, String status);
    long countByDateKey(String dateKey);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attendance a WHERE LOWER(a.userEmail) = LOWER(:userEmail)")
    void deleteByUserEmailIgnoreCase(@Param("userEmail") String userEmail);
}
