package com.empmanage.cms.repository;

import com.empmanage.cms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    long countByRole(String role);
    long countByRoleEntity_Id(Long roleId);
    long countByRoleEntity_NameIgnoreCase(String roleName);

    List<User> findByRoleEntity_Id(Long roleId);
    List<User> findByRole(String role);
}
