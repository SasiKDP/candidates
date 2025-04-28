package com.profile.candidate.dto;

public class DeleteCandidateResponseDto {
    private String status;
    private String message;
    private DeleteCandidateResponseDto.CandidateData data;
    private String error;

    // Constructor

    public DeleteCandidateResponseDto() {
    }
    public DeleteCandidateResponseDto(String status, String message, CandidateData data, String error) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.error = error;
    }
    // Getters and Setters
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Nested class for Payload
    public static class CandidateData {
        private String candidateId;
        private String candidateName;

        public CandidateData(String candidateId, String candidateName) {
            this.candidateId = candidateId;
            this.candidateName = candidateName;
        }

        public CandidateData() {
        }

        public String getCandidateId() {
            return candidateId;
        }

        public void setCandidateId(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getCandidateName() {
            return candidateName;
        }

        public void setCandidateName(String candidateName) {
            this.candidateName = candidateName;
        }
    }
}
