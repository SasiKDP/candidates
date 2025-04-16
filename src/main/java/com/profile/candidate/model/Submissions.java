package com.profile.candidate.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_submissions")
public class Submissions {

    @Id
    @Column(name = "submission_id", nullable = false, unique = true)
    private String submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateDetails candidate;

//    @OneToOne(mappedBy = "submission", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private InterviewDetails interviewDetails;


    @Column(nullable = false)
    private String jobId;

    private String resumeFilePath;

    @Lob
    @Column(name = "resume", columnDefinition = "LONGBLOB")
    private byte[] resume;

    private String preferredLocation;

    // Change the skills field from List<String> to String
    private String skills;  // Now it's just a single string

    private String clientName;

    private String communicationSkills;

    private Double requiredTechnologiesRating;

    private String overallFeedback;

    private LocalDate profileReceivedDate;

    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        this.submittedAt = LocalDateTime.now();

//        if (this.submissionId == null) {
//            generateSubmissionId();
//        }
    }
//    public void generateSubmissionId() {
//        if (candidate != null && candidate.getCandidateId() != null && jobId != null) {
//            this.submissionId = "SUBM-" + candidate.getCandidateId() + "-" + jobId;
//        } else {
//            throw new IllegalStateException("Candidate ID and Job ID must not be null to generate submission ID");
//        }
//    }

    public LocalDate getProfileReceivedDate() {
        return profileReceivedDate;
    }

    public void setProfileReceivedDate(LocalDate profileReceivedDate) {
        this.profileReceivedDate = profileReceivedDate;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
