package com.empmanage.cms.service;

import com.empmanage.cms.dto.PermissionDto;
import com.empmanage.cms.entity.Permission;
import com.empmanage.cms.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllByOrderByIdAsc();
    }

    public List<PermissionDto> getAllPermissionDtos() {
        return permissionRepository.findAllByOrderByIdAsc().stream()
                .map(p -> new PermissionDto(p.getId(), p.getName(), p.getModule(), p.getAction(), p.getDescription()))
                .collect(Collectors.toList());
    }

    public Map<String, List<PermissionDto>> getPermissionsGroupedByModule() {
        Map<String, List<PermissionDto>> map = new LinkedHashMap<>();
        List<Permission> list = permissionRepository.findAllByOrderByIdAsc();
        for (Permission p : list) {
            map.computeIfAbsent(p.getModule(), k -> new ArrayList<>())
               .add(new PermissionDto(p.getId(), p.getName(), p.getModule(), p.getAction(), p.getDescription()));
        }
        return map;
    }

    @Transactional
    public Permission createPermissionIfNotFound(String name, String module, String action, String description) {
        return permissionRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name, module, action, description)));
    }
}
