package com.empmanage.cms.service;

import com.empmanage.cms.entity.RoleAuditLog;
import com.empmanage.cms.repository.RoleAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleAuditLogService {

    @Autowired
    private RoleAuditLogRepository auditLogRepository;

    @Transactional
    public RoleAuditLog log(String action, String targetRole, String targetUser, String details) {
        String performedBy = "system";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            performedBy = auth.getName();
        }
        RoleAuditLog log = new RoleAuditLog(action, targetRole, targetUser, performedBy, details);
        return auditLogRepository.save(log);
    }

    public List<RoleAuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }
}
