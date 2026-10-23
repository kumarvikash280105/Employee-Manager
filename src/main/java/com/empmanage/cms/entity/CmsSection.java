package com.empmanage.cms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cms_sections")
public class CmsSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sectionKey; // "hero", "about", "contact", "metrics", "announcements", "footer"

    private String title;

    @Column(length = 1000)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String metaJson;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public CmsSection() {}

    public CmsSection(String sectionKey, String title, String subtitle, String content, String metaJson) {
        this.sectionKey = sectionKey;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.metaJson = metaJson;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSectionKey() { return sectionKey; }
    public void setSectionKey(String sectionKey) { this.sectionKey = sectionKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMetaJson() { return metaJson; }
    public void setMetaJson(String metaJson) { this.metaJson = metaJson; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
