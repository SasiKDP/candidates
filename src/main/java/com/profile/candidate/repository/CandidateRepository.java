package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<CandidateDetails, String> {

    // Fetch candidate by candidateId
    List<CandidateDetails> findAllByCandidateId(String candidateId);

    List<CandidateDetails> findByUserId(String userId);

    Optional<CandidateDetails> findByCandidateIdAndUserId(String candidateId, String userId);
    Optional<CandidateDetails> findByCandidateIdAndInterviewDateTime(String candidateId, OffsetDateTime interviewDateTime);
    Optional<CandidateDetails> findByCandidateEmailIdAndJobIdAndClientName(
            String candidateEmailId,
            String jobId,
            String clientName);

    Optional<CandidateDetails> findByContactNumberAndJobIdAndClientName(
            String contactNumber,
            String jobId,
            String clientEmail);
    // Method to fetch all candidates (this is already provided by JpaRepository)
    List<CandidateDetails> findAll();

    @Modifying
    @Transactional
    @Query(value = "UPDATE requirements_model_prod r SET r.status = 'Submitted' " +
            "WHERE r.job_id = :jobId AND EXISTS " +
            "(SELECT 1 FROM candidates_prod c WHERE c.job_id = :jobId)", nativeQuery = true)
    void updateRequirementStatus(@Param("jobId") String jobId);



    @Query("SELECT c FROM CandidateDetails c WHERE c.profileReceivedDate BETWEEN :startDate AND :endDate")
    List<CandidateDetails> findByProfileReceivedDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT c FROM CandidateDetails c WHERE c.userId = :userId AND c.profileReceivedDate BETWEEN :startDate AND :endDate")
    List<CandidateDetails> findByUserIdAndProfileReceivedDateBetween(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT c FROM CandidateDetails c " +
            "WHERE c.interviewDateTime IS NOT NULL " +
            "AND FUNCTION('DATE', c.timestamp) BETWEEN :startDate AND :endDate")
    List<CandidateDetails> findScheduledInterviewsByDateOnly(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    // Native SQL query to join candidates and requirements_model_prod tables based on jobId
    @Query(value = "SELECT r.client_name FROM requirements_model_prod r WHERE r.job_id = :jobId", nativeQuery = true)
    Optional<String> findClientNameByJobId(@Param("jobId") String jobId);


    @Query("SELECT c FROM CandidateDetails c WHERE c.userId = :userId AND c.timestamp BETWEEN :startDateTime AND :endDateTime")
    List<CandidateDetails> findScheduledInterviewsByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}