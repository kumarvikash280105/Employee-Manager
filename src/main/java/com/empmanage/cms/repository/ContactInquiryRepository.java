package com.empmanage.cms.repository;

import com.empmanage.cms.entity.ContactInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, Long> {
    List<ContactInquiry> findAllByOrderByCreatedAtDesc();
    long countByStatusIgnoreCase(String status);
}
