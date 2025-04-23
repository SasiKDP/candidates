package com.profile.candidate.dto;

import java.util.List;

public class TeamleadSubmissionsDTO {
    private List<CandidateGetResponseDto> selfSubmissions;
    private List<CandidateGetResponseDto> teamSubmissions;

    // Constructor accepting two lists
    public TeamleadSubmissionsDTO(List<CandidateGetResponseDto> selfSubmissions, List<CandidateGetResponseDto> teamSubmissions) {
        this.selfSubmissions = selfSubmissions;
        this.teamSubmissions = teamSubmissions;
    }

    // Getters and setters
    public List<CandidateGetResponseDto> getSelfSubmissions() {
        return selfSubmissions;
    }

    public void setSelfSubmissions(List<CandidateGetResponseDto> selfSubmissions) {
        this.selfSubmissions = selfSubmissions;
    }

    public List<CandidateGetResponseDto> getTeamSubmissions() {
        return teamSubmissions;
    }

    public void setTeamSubmissions(List<CandidateGetResponseDto> teamSubmissions) {
        this.teamSubmissions = teamSubmissions;
    }
}