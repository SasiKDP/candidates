package com.profile.candidate.repository;

import com.profile.candidate.model.PlacementDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
@Repository
public interface PlacementRepository extends JpaRepository<PlacementDetails, String> {

    boolean existsByPhone(String phone);
    boolean existsByConsultantEmail(String consultantEmail);
    @Query(value = "SELECT " +
            "(SELECT COUNT(*) FROM requirements_model) AS requirementsCount, " +
            "(SELECT COUNT(*) FROM candidates) AS candidatesCount, " +
            "(SELECT COUNT(*) FROM bdm_client) AS clientsCount, " +
            "(SELECT COUNT(*) FROM placements) AS placementsCount, " +
            "(SELECT COUNT(*) FROM bench_details) AS benchCount, " +
            "(SELECT COUNT(*) FROM user_details) AS usersCount, " +
            "(SELECT COUNT(*) FROM candidates WHERE interview_date_time IS NOT NULL) AS interviewsCount",
            nativeQuery = true)
    Object getAllCounts();
}
