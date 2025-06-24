package com.profile.candidate.dto;

public class CoordinatorInterviewUpdateDto {

    private String interviewStatus;

    private String internalFeedBack;

    private boolean skipNotification;

    public boolean isSkipNotification() {
        return skipNotification;
    }

    public void setSkipNotification(boolean skipNotification) {
        this.skipNotification = skipNotification;
    }

    public String getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public String getInternalFeedBack() {
        return internalFeedBack;
    }

    public void setInternalFeedBack(String internalFeedBack) {
        this.internalFeedBack = internalFeedBack;
    }
}
