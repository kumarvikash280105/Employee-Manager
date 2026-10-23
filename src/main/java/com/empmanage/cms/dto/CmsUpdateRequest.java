package com.empmanage.cms.dto;

public class CmsUpdateRequest {
    private String title;
    private String subtitle;
    private String content;
    private String metaJson;

    public CmsUpdateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMetaJson() { return metaJson; }
    public void setMetaJson(String metaJson) { this.metaJson = metaJson; }
}
