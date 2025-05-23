package com.profile.candidate.dto;

public class EncryptionRequestDTO {

    private String userId;

    private String placementId;

    private boolean newPlacement;


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlacementId() {
        return placementId;
    }

    public void setPlacementId(String placementId) {
        this.placementId = placementId;
    }

    public boolean isNewPlacement() {
        return newPlacement;
    }

    public void setNewPlacement(boolean newPlacement) {
        this.newPlacement = newPlacement;
    }
}
