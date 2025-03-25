package com.profile.candidate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class InterviewDto {

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    private String externalInterviewDetails;
    private String contactNumber;
    private String userEmail;
    private String interviewLevel;
    private String clientEmail;

    // Added interviewStatus field
    private String interviewStatus;

    // Constructor
    public InterviewDto(OffsetDateTime interviewDateTime, Integer duration, String zoomLink,
                        LocalDateTime scheduledTimeStamp, String userId, String jobId, String clientName,
                        String candidateId, String fullName, String externalInterviewDetails, String contactNumber,
                        String userEmail, String interviewLevel, String clientEmail, String interviewStatus) {
        this.interviewDateTime = interviewDateTime;
        this.duration = duration;
        this.zoomLink = zoomLink;
        this.scheduledTimeStamp = scheduledTimeStamp;
        this.userId = userId;
        this.jobId = jobId;
        this.clientName = clientName;
        this.candidateId = candidateId;
        this.fullName = fullName;
        this.externalInterviewDetails = externalInterviewDetails;
        this.contactNumber = contactNumber;
        this.userEmail = userEmail;
        this.interviewLevel = interviewLevel;
        this.clientEmail = clientEmail;
        this.interviewStatus = interviewStatus;
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

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
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

    @Override
    public String toString() {
        return "InterviewDto{" +
                "interviewDateTime=" + interviewDateTime +
                ", duration=" + duration +
                ", zoomLink='" + zoomLink + '\'' +
                ", scheduledTimeStamp=" + scheduledTimeStamp +
                ", userId='" + userId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", clientName='" + clientName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", interviewLevel='" + interviewLevel + '\'' +
                ", interviewStatus='" + interviewStatus + '\'' +  // Added interviewStatus in toString
                '}';
    }
}
