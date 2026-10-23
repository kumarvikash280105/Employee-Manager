package com.empmanage.cms.service;

import com.empmanage.cms.dto.CmsUpdateRequest;
import com.empmanage.cms.entity.CmsSection;
import com.empmanage.cms.repository.CmsSectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CmsService {

    @Autowired
    private CmsSectionRepository cmsSectionRepository;

    // Permitted CMS section keys whitelist (Requirement 6)
    private static final Set<String> PERMITTED_SECTIONS = Set.of("hero", "about", "contact", "features", "footer");

    // XSS sanitizer regex patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script.*?>.*?</script>|<script.*?>|onload=|onerror=|javascript:", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public List<CmsSection> getAllSections() {
        return cmsSectionRepository.findAll();
    }

    public Optional<CmsSection> getSection(String key) {
        if (key == null) return Optional.empty();
        return cmsSectionRepository.findBySectionKeyIgnoreCase(key.trim().toLowerCase());
    }

    @Transactional
    public CmsSection updateSection(String key, CmsUpdateRequest req) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("CMS section key is required.");
        }
        String cleanKey = key.trim().toLowerCase();

        // Validate section key against permitted whitelist (Requirement 6)
        if (!PERMITTED_SECTIONS.contains(cleanKey)) {
            throw new IllegalArgumentException("Invalid CMS section key '" + cleanKey + "'. Permitted sections are: " + PERMITTED_SECTIONS);
        }

        CmsSection section = cmsSectionRepository.findBySectionKeyIgnoreCase(cleanKey)
            .orElseGet(() -> {
                CmsSection s = new CmsSection();
                s.setSectionKey(cleanKey);
                return s;
            });

        // XSS sanitization on incoming inputs
        if (req.getTitle() != null) {
            section.setTitle(sanitizeXss(req.getTitle().trim()));
        }
        if (req.getSubtitle() != null) {
            section.setSubtitle(sanitizeXss(req.getSubtitle().trim()));
        }
        if (req.getContent() != null) {
            section.setContent(sanitizeXss(req.getContent().trim()));
        }
        if (req.getMetaJson() != null) {
            section.setMetaJson(sanitizeXss(req.getMetaJson().trim()));
        }
        section.setUpdatedAt(LocalDateTime.now());

        return cmsSectionRepository.save(section);
    }

    private String sanitizeXss(String input) {
        if (input == null) return null;
        return SCRIPT_PATTERN.matcher(input).replaceAll("");
    }
}
