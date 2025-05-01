package com.profile.candidate.dto;


import java.util.List;

public class TeamleadSubmissionsDTO {
    private List<SubmissionGetResponseDto> selfSubmissions;
    private List<SubmissionGetResponseDto> teamSubmissions;

    // Constructor accepting two lists
    public TeamleadSubmissionsDTO(List<SubmissionGetResponseDto> selfSubmissions, List<SubmissionGetResponseDto> teamSubmissions) {
        this.selfSubmissions = selfSubmissions;
        this.teamSubmissions = teamSubmissions;
    }

    // Getters and setters
    public List<SubmissionGetResponseDto> getSelfSubmissions() {
        return selfSubmissions;
    }

    public void setSelfSubmissions(List<SubmissionGetResponseDto> selfSubmissions) {
        this.selfSubmissions = selfSubmissions;
    }

    public List<SubmissionGetResponseDto> getTeamSubmissions() {
        return teamSubmissions;
    }

    public void setTeamSubmissions(List<SubmissionGetResponseDto> teamSubmissions) {
        this.teamSubmissions = teamSubmissions;
    }
}
