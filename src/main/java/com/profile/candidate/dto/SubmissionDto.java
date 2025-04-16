package com.profile.candidate.dto;

import com.profile.candidate.model.CandidateDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubmissionDto {

    private String submissionId;

    private CandidateDetails candidate;

    private String jobId;

    private LocalDate profileReceivedDate;

    private byte[] resume;  // Storing file as byte array in DB

    private String resumeFilePath;

    private String preferredLocation;

    // Change the skills field from List<String> to String
    private String skills;  // Now it's just a single string

    private String communicationSkills;

    private Double requiredTechnologiesRating;

    private String overallFeedback;

    private LocalDateTime submittedAt;


    public LocalDate getProfileReceivedDate() {
        return profileReceivedDate;
    }

    public void setProfileReceivedDate(LocalDate profileReceivedDate) {
        this.profileReceivedDate = profileReceivedDate;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getCommunicationSkills() {
        return communicationSkills;
    }

    public void setCommunicationSkills(String communicationSkills) {
        this.communicationSkills = communicationSkills;
    }

    public Double getRequiredTechnologiesRating() {
        return requiredTechnologiesRating;
    }

    public void setRequiredTechnologiesRating(Double requiredTechnologiesRating) {
        this.requiredTechnologiesRating = requiredTechnologiesRating;
    }

    public String getOverallFeedback() {
        return overallFeedback;
    }

    public void setOverallFeedback(String overallFeedback) {
        this.overallFeedback = overallFeedback;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public CandidateDetails getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateDetails candidate) {
        this.candidate = candidate;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getResumeFilePath() {
        return resumeFilePath;
    }

    public void setResumeFilePath(String resumeFilePath) {
        this.resumeFilePath = resumeFilePath;
    }

    public byte[] getResume() {
        return resume;
    }

    public void setResume(byte[] resume) {
        this.resume = resume;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

}
