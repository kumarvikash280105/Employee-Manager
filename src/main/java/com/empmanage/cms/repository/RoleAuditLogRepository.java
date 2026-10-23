package com.empmanage.cms.repository;

import com.empmanage.cms.entity.RoleAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLog, Long> {

    List<RoleAuditLog> findTop50ByOrderByTimestampDesc();

    List<RoleAuditLog> findAllByOrderByTimestampDesc();
}
