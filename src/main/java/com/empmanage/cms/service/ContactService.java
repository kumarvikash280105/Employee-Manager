package com.empmanage.cms.service;

import com.empmanage.cms.entity.ContactInquiry;
import com.empmanage.cms.repository.ContactInquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    @Autowired
    private ContactInquiryRepository contactInquiryRepository;

    public ContactInquiry submitInquiry(ContactInquiry inquiry) {
        if (inquiry.getName() == null || inquiry.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (inquiry.getEmail() == null || inquiry.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (inquiry.getSubject() == null || inquiry.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (inquiry.getMessage() == null || inquiry.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        inquiry.setStatus("PENDING");
        return contactInquiryRepository.save(inquiry);
    }

    public List<ContactInquiry> getAllInquiries() {
        return contactInquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    public ContactInquiry updateStatus(Long id, String status) {
        ContactInquiry inq = contactInquiryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Inquiry not found with ID: " + id));

        inq.setStatus(status.toUpperCase());
        return contactInquiryRepository.save(inq);
    }

    public void deleteInquiry(Long id) {
        contactInquiryRepository.deleteById(id);
    }

    public long getPendingCount() {
        return contactInquiryRepository.countByStatusIgnoreCase("PENDING");
    }
}
