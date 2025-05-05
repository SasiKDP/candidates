package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<CandidateDetails, String> {
    // Additional custom queries if needed
    // Find candidate by email
    Optional<CandidateDetails> findByCandidateEmailId(String candidateEmailId);
    List<CandidateDetails> findByUserId(String userId);

    Optional<CandidateDetails> findByCandidateIdAndUserId(String candidateId, String userId);
    // Method to fetch all candidates (this is already provided by JpaRepository)
    List<CandidateDetails> findAll();
    // Native SQL query to join candidates and requirements_model_prod tables based on jobId
    @Query(value = "SELECT r.client_name FROM requirements_model r WHERE r.job_id = :jobId", nativeQuery = true)
    Optional<String> findClientNameByJobId(@Param("jobId") String jobId);

    @Query(value = "SELECT u.email FROM user_details u " +
            "JOIN requirements_model r ON r.assigned_by = u.user_name " +
            "WHERE r.job_id = :jobId", nativeQuery = true)
    String findTeamLeadEmailByJobId(@Param("jobId") String jobId);

    @Query(value = "SELECT u.user_name FROM user_details u WHERE u.email = :email", nativeQuery = true)
    String findUserNameByEmail(@Param("email") String email);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(candidate_id, 5) AS UNSIGNED)) FROM candidates", nativeQuery = true)
    Integer findMaxCandidateNumber();


}