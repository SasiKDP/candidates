package com.profile.candidate.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class GetInterviewResponseDto {

    private String interviewId;
    private String jobId;                   // Job ID
    private String candidateId;             // Candidate ID
    private String candidateFullName;       // Candidate Full Name
    private String candidateContactNo;      // Candidate Contact Number
    private String candidateEmailId;                 // Candidate Email ID
    private String userEmail;               // User's Email
    private String userId;                  // User ID (interviewer)
    private String skills;
    private float totalExperience;
    private float relevantExperience;
    private OffsetDateTime interviewDateTime; // Interview Date & Time
    private Integer duration;               // Duration of the interview
    private String zoomLink;                // Zoom Link for the interview
    private LocalDateTime interviewScheduledTimestamp; // Timestamp when interview is scheduled
    private List<String> clientEmail;             // Client Email
    private String clientName;              // Client Name
    private String interviewLevel;          // Interview Level (e.g., 1st, 2nd round)
    private String latestInterviewStatus;
    private String recruiterName;
    private boolean isPlaced;// New field for status
    private String technology;
    private String internalFeedback;
    private String comments;

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public float getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(float totalExperience) {
        this.totalExperience = totalExperience;
    }

    public float getRelevantExperience() {
        return relevantExperience;
    }

    public void setRelevantExperience(float relevantExperience) {
        this.relevantExperience = relevantExperience;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public GetInterviewResponseDto(String interviewId, String jobId, String candidateId, String candidateFullName, String candidateContactNo, String candidateEmailId, String userEmail, String userId, String skills, float totalExperience, float relevantExperience, OffsetDateTime interviewDateTime, Integer duration, String zoomLink, LocalDateTime interviewScheduledTimestamp, List<String> clientEmail, String clientName, String interviewLevel, String latestInterviewStatus, String recruiterName, boolean isPlaced, String technology, String internalFeedback, String comments) {
        this.interviewId = interviewId;
        this.jobId = jobId;
        this.candidateId = candidateId;
        this.candidateFullName = candidateFullName;
        this.candidateContactNo = candidateContactNo;
        this.candidateEmailId = candidateEmailId;
        this.userEmail = userEmail;
        this.userId = userId;
        this.skills = skills;
        this.totalExperience = totalExperience;
        this.relevantExperience = relevantExperience;
        this.interviewDateTime = interviewDateTime;
        this.duration = duration;
        this.zoomLink = zoomLink;
        this.interviewScheduledTimestamp = interviewScheduledTimestamp;
        this.clientEmail = clientEmail;
        this.clientName = clientName;
        this.interviewLevel = interviewLevel;
        this.latestInterviewStatus = latestInterviewStatus;
        this.recruiterName = recruiterName;
        this.isPlaced = isPlaced;
        this.technology = technology;
        this.internalFeedback = internalFeedback;
        this.comments = comments;
    }

    public boolean getIsPlaced() {
        return isPlaced;
    }

    public void setIsPlaced(boolean isPlaced) {
        this.isPlaced = isPlaced;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateFullName() {
        return candidateFullName;
    }

    public void setCandidateFullName(String candidateFullName) {
        this.candidateFullName = candidateFullName;
    }

    public String getCandidateContactNo() {
        return candidateContactNo;
    }

    public void setCandidateContactNo(String candidateContactNo) {
        this.candidateContactNo = candidateContactNo;
    }

    public String getCandidateEmailId() {
        return candidateEmailId;
    }

    public void setCandidateEmailId(String candidateEmailId) {
        this.candidateEmailId = candidateEmailId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

    public String getZoomLink() {
        return zoomLink;
    }

    public void setZoomLink(String zoomLink) {
        this.zoomLink = zoomLink;
    }

    public LocalDateTime getInterviewScheduledTimestamp() {
        return interviewScheduledTimestamp;
    }

    public void setInterviewScheduledTimestamp(LocalDateTime interviewScheduledTimestamp) {
        this.interviewScheduledTimestamp = interviewScheduledTimestamp;
    }

    public String getLatestInterviewStatus() {
        return latestInterviewStatus;
    }

    public void setLatestInterviewStatus(String latestInterviewStatus) {
        this.latestInterviewStatus = latestInterviewStatus;
    }

    public List<String> getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(List<String> clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getInterviewLevel() {
        return interviewLevel;
    }

    public void setInterviewLevel(String interviewLevel) {
        this.interviewLevel = interviewLevel;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public boolean isPlaced() {
        return isPlaced;
    }

    public void setPlaced(boolean placed) {
        isPlaced = placed;
    }

    public String getInternalFeedback() {
        return internalFeedback;
    }

    public void setInternalFeedback(String internalFeedback) {
        this.internalFeedback = internalFeedback;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "GetInterviewResponseDto{" +
                "jobId='" + jobId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", candidateFullName='" + candidateFullName + '\'' +
                ", candidateContactNo='" + candidateContactNo + '\'' +
                ", emailId='" + candidateEmailId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userId='" + userId + '\'' +
                ", interviewDateTime=" + interviewDateTime +
                ", duration=" + duration +
                ", zoomLink='" + zoomLink + '\'' +
                ", interviewScheduledTimestamp=" + interviewScheduledTimestamp +
                ", clientEmail='" + clientEmail + '\'' +
                ", clientName='" + clientName + '\'' +
                ", interviewLevel='" + interviewLevel + '\'' +
                '}';
    }
}
