package com.Edo_perfume.ScentOfASA.slot.dto;

public class AdminSlotResponse {

    private Long id;
    private String timeSlot;
    private String guideLanguage;
    private Long guideStaffId;
    private String guideName;
    private String slotStatus;
    private String effectiveStatus;
    private boolean available;
    private int remainingCapacity;
    private int reservedGuestCount;

    public AdminSlotResponse() {
    }

    public AdminSlotResponse(Long id, String timeSlot, String guideLanguage, Long guideStaffId, String guideName,
                             String slotStatus, String effectiveStatus, boolean available) {
        this(id, timeSlot, guideLanguage, guideStaffId, guideName, slotStatus, effectiveStatus, available, 0, 0);
    }

    public AdminSlotResponse(Long id, String timeSlot, String guideLanguage, Long guideStaffId, String guideName,
                             String slotStatus, String effectiveStatus, boolean available,
                             int remainingCapacity, int reservedGuestCount) {
        this.id = id;
        this.timeSlot = timeSlot;
        this.guideLanguage = guideLanguage;
        this.guideStaffId = guideStaffId;
        this.guideName = guideName;
        this.slotStatus = slotStatus;
        this.effectiveStatus = effectiveStatus;
        this.available = available;
        this.remainingCapacity = remainingCapacity;
        this.reservedGuestCount = reservedGuestCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getGuideLanguage() {
        return guideLanguage;
    }

    public void setGuideLanguage(String guideLanguage) {
        this.guideLanguage = guideLanguage;
    }

    public String getGuideName() {
        return guideName;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public Long getGuideStaffId() {
        return guideStaffId;
    }

    public void setGuideStaffId(Long guideStaffId) {
        this.guideStaffId = guideStaffId;
    }

    public String getSlotStatus() {
        return slotStatus;
    }

    public void setSlotStatus(String slotStatus) {
        this.slotStatus = slotStatus;
    }

    public String getEffectiveStatus() {
        return effectiveStatus;
    }

    public void setEffectiveStatus(String effectiveStatus) {
        this.effectiveStatus = effectiveStatus;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public int getReservedGuestCount() {
        return reservedGuestCount;
    }

    public void setReservedGuestCount(int reservedGuestCount) {
        this.reservedGuestCount = reservedGuestCount;
    }
}
