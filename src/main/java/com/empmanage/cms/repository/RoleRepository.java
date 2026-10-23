package com.empmanage.cms.repository;

import com.empmanage.cms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Role> findAllByOrderByCreatedAtAsc();

    long countByActiveTrue();
}
