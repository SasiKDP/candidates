package com.profile.candidate.dto;

public interface DashboardCountsProjection {
    Long getRequirementsCount();
    Long getCandidatesCount();
    Long getClientsCount();
    Long getPlacementsCount();
    Long getBenchCount();
    Long getUsersCount();
    Long getInterviewsCount();
}