package com.Edo_perfume.ScentOfASA.guide.dto;

public class GuideProfileResponse {

    private Long id;
    private String loginId;
    private String displayName;
    private String guideLanguage;

    public GuideProfileResponse() {
    }

    public GuideProfileResponse(Long id, String loginId, String displayName, String guideLanguage) {
        this.id = id;
        this.loginId = loginId;
        this.displayName = displayName;
        this.guideLanguage = guideLanguage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGuideLanguage() {
        return guideLanguage;
    }

    public void setGuideLanguage(String guideLanguage) {
        this.guideLanguage = guideLanguage;
    }
}
