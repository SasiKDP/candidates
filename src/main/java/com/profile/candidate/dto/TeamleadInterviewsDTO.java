package com.profile.candidate.dto;

import java.util.List;

public class TeamleadInterviewsDTO {
    private List<GetInterviewResponseDto> selfInterviews;   // List of self interviews
    private List<GetInterviewResponseDto> teamInterviews;   // List of team interviews

    // Constructor
    public TeamleadInterviewsDTO(List<GetInterviewResponseDto> selfInterviews, List<GetInterviewResponseDto> teamInterviews) {
        this.selfInterviews = selfInterviews;
        this.teamInterviews = teamInterviews;
    }

    // Getters and Setters
    public List<GetInterviewResponseDto> getSelfInterviews() {
        return selfInterviews;
    }

    public void setSelfInterviews(List<GetInterviewResponseDto> selfInterviews) {
        this.selfInterviews = selfInterviews;
    }

    public List<GetInterviewResponseDto> getTeamInterviews() {
        return teamInterviews;
    }

    public void setTeamInterviews(List<GetInterviewResponseDto> teamInterviews) {
        this.teamInterviews = teamInterviews;
    }
}
