package com.Edo_perfume.ScentOfASA.guide.dto;

public class GuideSlotAssignmentRequest {

    private String loginId;
    private Boolean assigned;
    private String slotStatus;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public Boolean getAssigned() {
        return assigned;
    }

    public void setAssigned(Boolean assigned) {
        this.assigned = assigned;
    }

    public String getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(String slotStatus) {
        this.slotStatus = slotStatus;
    }
}
