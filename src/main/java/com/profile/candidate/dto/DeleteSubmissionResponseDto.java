package com.profile.candidate.dto;

public class DeleteSubmissionResponseDto {

    private boolean status;
    private String message;
    private SubmissionData data;
    private String error;

    // Constructor
    public DeleteSubmissionResponseDto(boolean status, String message, SubmissionData data, String error) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.error = error;

    }

    // Getters and Setters
    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public SubmissionData getPayload() {
        return data;
    }

    public void setPayload(SubmissionData data) {
        this.data = data;
    }

    // Nested class for Payload
    public static class SubmissionData {
        private String candidateId;
        private String jobId;

        // Constructor
        public SubmissionData(String candidateId, String jobId) {
            this.candidateId = candidateId;
            this.jobId = jobId;
        }

        // Getters and Setters
        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }
    }
}
