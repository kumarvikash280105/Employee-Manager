package com.empmanage.cms.repository;

import com.empmanage.cms.entity.CmsSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CmsSectionRepository extends JpaRepository<CmsSection, Long> {
    Optional<CmsSection> findBySectionKeyIgnoreCase(String sectionKey);
    boolean existsBySectionKeyIgnoreCase(String sectionKey);
}
