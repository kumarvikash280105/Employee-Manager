package com.empmanage.cms.service;

import com.empmanage.cms.dto.CmsUpdateRequest;
import com.empmanage.cms.entity.CmsSection;
import com.empmanage.cms.repository.CmsSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CmsServiceTest {

    @Mock
    private CmsSectionRepository cmsSectionRepository;

    @InjectMocks
    private CmsService cmsService;

    @Test
    @DisplayName("CMS Management - Update section successfully with whitelisted key")
    void updateSection_success() {
        CmsUpdateRequest req = new CmsUpdateRequest();
        req.setTitle("Enterprise Employee Platform");
        req.setSubtitle("Modern workforce intelligence");

        when(cmsSectionRepository.findBySectionKeyIgnoreCase("hero")).thenReturn(Optional.of(new CmsSection("hero", "Old Title", "Old Sub", "Content", null)));
        when(cmsSectionRepository.save(any(CmsSection.class))).thenAnswer(inv -> inv.getArgument(0));

        CmsSection updated = cmsService.updateSection("hero", req);

        assertNotNull(updated);
        assertEquals("Enterprise Employee Platform", updated.getTitle());
        assertEquals("Modern workforce intelligence", updated.getSubtitle());
        verify(cmsSectionRepository, times(1)).save(any(CmsSection.class));
    }

    @Test
    @DisplayName("Requirement 6 - Reject CMS update with invalid / unwhitelisted section key")
    void updateSection_invalidSectionKey_throwsException() {
        CmsUpdateRequest req = new CmsUpdateRequest();
        req.setTitle("Hacked Section");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cmsService.updateSection("malicious_section", req));
        assertTrue(ex.getMessage().contains("Invalid CMS section key"));
        verify(cmsSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement 6 - XSS Sanitization strips malicious script tags and event handlers")
    void updateSection_xssSanitization() {
        CmsUpdateRequest req = new CmsUpdateRequest();
        req.setTitle("Welcome <script>alert('XSS')</script> to EmpManage");
        req.setContent("Explore <img src='x' onerror='stealCookies()'> features");

        when(cmsSectionRepository.findBySectionKeyIgnoreCase("about")).thenReturn(Optional.of(new CmsSection("about", "", "", "", null)));
        when(cmsSectionRepository.save(any(CmsSection.class))).thenAnswer(inv -> inv.getArgument(0));

        CmsSection updated = cmsService.updateSection("about", req);

        assertNotNull(updated);
        assertFalse(updated.getTitle().contains("<script>"));
        assertFalse(updated.getTitle().contains("</script>"));
        assertFalse(updated.getContent().contains("onerror="));
    }
}
