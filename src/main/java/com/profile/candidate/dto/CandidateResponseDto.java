package com.profile.candidate.dto;

public class CandidateResponseDto {

    private String status;  // Success or Error
    private String message;  // Message to describe the status
    private CandidateResponseDto.CandidateData data;  // Contains candidateId, employeeId, and jobId
    private String errorMessage;  // Error message in case of failure

    // Constructor

    public CandidateResponseDto() {
    }

    public CandidateResponseDto(String status, String message, CandidateData data, String errorMessage) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CandidateData getData() {
        return data;
    }

    public void setData(CandidateData data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Inner Payload class to represent candidateId, employeeId, and jobId
    public static class CandidateData {
        private String candidateId;
        private String employeeId;
        private String submissionId;
        // Constructor

        public CandidateData() {
        }

        public CandidateData(String candidateId, String employeeId, String submissionId) {
            this.candidateId = candidateId;
            this.employeeId = employeeId;
            this.submissionId = submissionId;
        }

        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getSubmissionId() {
            return submissionId;
        }

        public void setSubmissionId(String submissionId) {
            this.submissionId = submissionId;
        }
    }
}
