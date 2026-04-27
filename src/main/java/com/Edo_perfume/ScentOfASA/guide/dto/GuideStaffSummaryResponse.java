package com.Edo_perfume.ScentOfASA.guide.dto;

public class GuideStaffSummaryResponse {

    private Long id;
    private String loginId;
    private String displayName;
    private String guideLanguage;

    public GuideStaffSummaryResponse(Long id, String loginId, String displayName, String guideLanguage) {
        this.id = id;
        this.loginId = loginId;
        this.displayName = displayName;
        this.guideLanguage = guideLanguage;
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGuideLanguage() {
        return guideLanguage;
    }
}
