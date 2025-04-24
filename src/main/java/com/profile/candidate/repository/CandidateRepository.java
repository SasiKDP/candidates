package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import jakarta.persistence.Tuple;
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

    @Query("SELECT MAX(c.candidateId) FROM CandidateDetails c WHERE c.candidateId LIKE 'CAND%'")
    String findMaxCandidateId();

    // Fetch candidate by candidateId
    List<CandidateDetails> findAllByCandidateId(String candidateId);

    // Native query to find the user name by email from the user_details_prod table
    @Query(value = "SELECT user_name FROM user_details_prod WHERE email = :email", nativeQuery = true)
    String findUserNameByEmail(@Param("email") String email);


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

    @Query(value = "SELECT u.email FROM user_details_prod u " +
            "JOIN requirements_model_prod r ON LOWER(r.assigned_by) = LOWER(u.user_name) " +
            "WHERE r.job_id = :jobId", nativeQuery = true)
    String findTeamLeadEmailByJobId(@Param("jobId") String jobId);

    @Query(value = """
    SELECT * FROM candidates_prod c
    WHERE c.job_id IN (
        SELECT r.job_id
        FROM requirements_model_prod r
        JOIN bdm_client_prod b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        JOIN user_details_prod u 
            ON b.on_boarded_by = u.user_name
        WHERE u.user_id = :userId
    )
    AND c.profile_received_date BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<CandidateDetails> findSubmissionsByBdmUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT r.name
    FROM user_roles_prod ur
    JOIN roles_prod r ON ur.role_id = r.id
    WHERE ur.user_id = :userId
    LIMIT 1
""", nativeQuery = true)
    String findRoleByUserId(@Param("userId") String userId);
    @Query(value = """
SELECT 
    c.job_id,
    c.candidate_id,
    c.full_name,
    c.contact_number,
    c.candidate_email_id,
    c.user_email,
    c.user_id,
    DATE_FORMAT(c.interview_date_time, '%Y-%m-%dT%H:%i:%s') AS interview_date_time,
    c.duration,
    c.zoom_link,
    DATE_FORMAT(c.timestamp, '%Y-%m-%dT%H:%i:%s') AS timestamp,
    c.client_email,
    c.client_name,
    c.interview_level,
    c.interview_status
FROM 
    candidates_prod c
WHERE 
    c.job_id IN (
        SELECT r.job_id
        FROM requirements_model_prod r
        JOIN bdm_client_prod b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        JOIN user_details_prod u 
            ON b.on_boarded_by = u.user_name
        WHERE u.user_id = :userId
    )
    AND c.interview_date_time IS NOT NULL
    AND c.timestamp BETWEEN :startDateTime AND :endDateTime
""", nativeQuery = true)
    List<Tuple> findScheduledInterviewsByBdmUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}