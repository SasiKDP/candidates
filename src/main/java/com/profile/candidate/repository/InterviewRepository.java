package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.InterviewDetails;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<InterviewDetails,String> {


    @Query(value = "SELECT id FROM `dataquad-prod`.bdm_client_prod WHERE client_name = :clientName LIMIT 1", nativeQuery = true)
    String findClientIdByClientName(@Param("clientName") String clientName);

    InterviewDetails findByCandidateIdAndUserId(String candidateId, String userId);

    @Query("SELECT i FROM InterviewDetails i WHERE i.candidateId = :candidateId")
    List<InterviewDetails> findInterviewsByCandidateId(@Param("candidateId") String candidateId);

    InterviewDetails findByCandidateIdAndUserIdAndClientName(String candidateId, String userId, String clientName);

    InterviewDetails findByCandidateIdAndUserIdAndClientNameAndJobId(String candidateId, String userId, String clientName, String jobId);

    InterviewDetails findInterviewsByCandidateIdAndJobId(String  candidateId,String jobId);

   InterviewDetails findByCandidateIdAndUserIdAndJobId(String candidateId, String userId, String jobId);

    Optional<InterviewDetails> findByCandidateIdAndJobIdAndInterviewDateTime(String candidateId,String jobId, OffsetDateTime interviewDateTime);

    List<InterviewDetails> findByUserId(String userId);

    InterviewDetails findByCandidateIdAndClientNameAndJobId(String candidateId, String clientName, String jobId);

    InterviewDetails findByCandidateIdAndJobId(String candidateId, String jobId);

    InterviewDetails findByCandidateIdAndClientName(String candidateId, String clientName);

    @Query(value = "SELECT r.job_title FROM `dataquad-prod`.requirements_model_prod r WHERE r.job_id = :jobId", nativeQuery = true)
    String findJobTitleByJobId(@Param("jobId") String jobId);

    @Query(value = "SELECT user_name FROM `dataquad-prod`.user_details_prod WHERE user_id = :userId", nativeQuery = true)
    String findUsernameByUserId(@Param("userId") String userId);

    @Query("SELECT i FROM InterviewDetails i WHERE i.userId = :userId AND i.timestamp BETWEEN :startDateTime AND :endDateTime")
    List<InterviewDetails> findScheduledInterviewsByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);


    @Query("SELECT i FROM InterviewDetails i " +
            "WHERE i.interviewDateTime IS NOT NULL " +
            "AND FUNCTION('DATE', i.timestamp) BETWEEN :startDate AND :endDate")
    List<InterviewDetails> findScheduledInterviewsByDateOnly(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query(value = """
    SELECT r.name
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
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
    c.interview_status,
    c.is_placed 
FROM 
    interview_details c
WHERE 
    c.job_id IN (
        SELECT r.job_id
        FROM requirements_model r
        JOIN bdm_client b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        JOIN user_details u 
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

    @Query(value = """
                SELECT c.* 
                FROM interview_details c
                JOIN requirements_model r ON c.job_id = r.job_id
                WHERE c.user_id = :userId
                  AND c.interview_date_time IS NOT NULL
                  AND c.client_name = r.client_name  -- Ensures client_name matches between candidates and requirements
                  AND DATE(c.timestamp) BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    List<InterviewDetails> findSelfScheduledInterviewsByTeamleadAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
                SELECT c.* 
                FROM user_details u
                JOIN requirements_model r ON r.assigned_by = u.user_name
                JOIN interview_details c ON c.job_id = r.job_id
                WHERE u.user_id = :userId
                  AND c.user_id != u.user_id
                  AND c.interview_date_time IS NOT NULL
                  AND c.job_id IN (
                      SELECT r2.job_id 
                      FROM requirements_model r2
                      WHERE r2.client_name = r.client_name 
                        AND r2.assigned_by = u.user_name
                  )  -- Ensures client_name matches between candidates and requirements
                  AND DATE(c.timestamp) BETWEEN :startDate AND :endDate  -- Date only filter using timestamp
            """, nativeQuery = true)
    List<InterviewDetails> findTeamScheduledInterviewsByTeamleadAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
