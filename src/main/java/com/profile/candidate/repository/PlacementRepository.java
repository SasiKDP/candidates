package com.profile.candidate.repository;

import com.profile.candidate.model.PlacementDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementRepository extends JpaRepository<PlacementDetails, String> {
    boolean existsByClientEmail(String clientEmail);
    boolean existsByCandidateContactNo(String candidateContactNo);
    @Query(value = "SELECT " +
            "(SELECT COUNT(*) FROM requirements_model_prod) AS requirementsCount, " +
            "(SELECT COUNT(*) FROM candidates_prod) AS candidatesCount, " +
            "(SELECT COUNT(*) FROM bdm_client_prod) AS clientsCount, " +
            "(SELECT COUNT(*) FROM placement_prod) AS placementsCount, " +
            "(SELECT COUNT(*) FROM bench_details) AS benchCount, " +
            "(SELECT COUNT(*) FROM user_details_prod) AS usersCount, " +
            "(SELECT COUNT(*) FROM candidates_prod WHERE interview_date_time IS NOT NULL) AS interviewsCount",
            nativeQuery = true)
    Object getAllCounts();
}