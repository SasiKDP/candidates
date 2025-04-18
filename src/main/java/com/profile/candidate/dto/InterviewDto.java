package com.profile.candidate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class InterviewDto {


    private boolean sentEmails;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime interviewDateTime;
    private Integer duration;
    private String zoomLink;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime scheduledTimeStamp;

    private String userId;
    private String jobId;
    private String clientName;
    private String candidateId;
    private String fullName;
    private String externalInterviewDetails;
    private String contactNumber;
    private String userEmail;
    private String interviewLevel;

    private List<String> clientEmail;

    private String candidateEmailId;

    // Added interviewStatus field
    private String interviewStatus;

    // Constructor


    public boolean isSentEmails() {
        return sentEmails;
    }

    public void setSentEmails(boolean sentEmails) {
        this.sentEmails = sentEmails;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getCandidateEmailId() {
        return candidateEmailId;
    }

    public void setCandidateEmailId(String candidateEmailId) {
        this.candidateEmailId = candidateEmailId;
    }

    // Getters and Setters
    public OffsetDateTime getInterviewDateTime() {
        return interviewDateTime;
    }

    public void setInterviewDateTime(OffsetDateTime interviewDateTime) {
        this.interviewDateTime = interviewDateTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDateTime getScheduledTimeStamp() {
        return scheduledTimeStamp;
    }

    public void setScheduledTimeStamp(LocalDateTime scheduledTimeStamp) {
        this.scheduledTimeStamp = scheduledTimeStamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getInterviewLevel() {
        return interviewLevel;
    }

    public void setInterviewLevel(String interviewLevel) {
        this.interviewLevel = interviewLevel;
    }

    public String getZoomLink() {
        return zoomLink;
    }

    public void setZoomLink(String zoomLink) {
        this.zoomLink = zoomLink;
    }

    public String getExternalInterviewDetails() {
        return externalInterviewDetails;
    }

    public void setExternalInterviewDetails(String externalInterviewDetails) {
        this.externalInterviewDetails = externalInterviewDetails;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getInterviewStatus() {
        return interviewStatus;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

    public List<String> getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(List<String> clientEmail) {
        this.clientEmail = clientEmail;
    }

    @Override
    public String toString() {
        return "InterviewDto{" +
                "sentEmails=" + sentEmails +
                ", interviewDateTime=" + interviewDateTime +
                ", duration=" + duration +
                ", zoomLink='" + zoomLink + '\'' +
                ", scheduledTimeStamp=" + scheduledTimeStamp +
                ", userId='" + userId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", externalInterviewDetails='" + externalInterviewDetails + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", interviewLevel='" + interviewLevel + '\'' +
                ", clientEmail=" + clientEmail +
                ", candidateEmailId='" + candidateEmailId + '\'' +
                ", interviewStatus='" + interviewStatus + '\'' +
                '}';
    }
}