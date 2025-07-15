package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submissions,String> {


     List<Submissions> findByCandidate_CandidateId(String candidateId);

     Submissions findByCandidate_CandidateIdAndJobId(String candidateId, String jobId);

    Optional<Submissions> findBySubmissionIdAndUserId(String candidateId, String userId);

    List<Submissions> findByCandidate_CandidateIdIn(List<String> candidateIds);

     List<Submissions> findByCandidate(CandidateDetails candidate);

    Submissions findByCandidate_ContactNumberAndJobId(String contactNumber, String jobId);

    Submissions findByCandidate_CandidateEmailIdAndJobId(String candidateId, String jobId);

    @Query(value = "SELECT r.job_title FROM requirements_model r WHERE r.job_id = :jobId", nativeQuery = true)
    String findJobTitleByJobId(@Param("jobId") String jobId);

    @Query("SELECT s.candidate.candidateId FROM Submissions s WHERE s.submissionId = :submissionId")
    String findCandidateIdBySubmissionId(@Param("submissionId") String submissionId);

    @Query(value = """
    SELECT r.name
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.id
    WHERE ur.user_id = :userId
    LIMIT 1
""", nativeQuery = true)
    String findRoleByUserId(@Param("userId") String userId);
    @Query("SELECT s FROM Submissions s  WHERE s.userId = :userId AND s.profileReceivedDate BETWEEN :startDate AND :endDate")
    List<Submissions> findByUserIdAndProfileReceivedDateBetween(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT * FROM candidate_submissions c
    WHERE c.job_id IN (
        SELECT r.job_id
        FROM requirements_model r
        JOIN bdm_client b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        JOIN user_details u 
            ON b.on_boarded_by = u.user_name
        WHERE u.user_id = :userId
    )
    AND c.profile_received_date BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<Submissions> findSubmissionsByBdmUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    List<Submissions> findByProfileReceivedDateBetween(LocalDate start, LocalDate end);


	@Query(value = """    
SELECT 
    cs.submission_id,     
    cs.candidate_id AS candidate_id,       
    cs.recruiter_name as recruiter_name,   
    c.full_name AS full_name,    
    cs.skills AS skills,      
    cs.job_id AS job_id,
    cs.user_id AS user_id,
    cs.user_email AS user_email,
    cs.preferred_location AS preferred_location,
    DATE_FORMAT(cs.profile_received_date, '%Y-%m-%d') AS profile_received_date,
    r.job_title AS job_title,    
    r.client_name AS client_name,
    c.contact_number AS contact_number,      
    c.candidate_email_id AS candidate_email_id,  
    c.total_experience AS total_experience,   
    c.relevant_experience AS relevant_experience,
    c.current_organization AS current_organization,
    c.qualification AS qualification,
    c.currentctc AS current_ctc,
    c.expectedctc AS expected_ctc,
    c.notice_period AS notice_period,
    c.current_location AS current_location,
    cs.communication_skills AS communication_skills,
    cs.required_technologies_rating AS required_technologies_rating,
    cs.overall_feedback AS overall_feedback,
    cs.submitted_at AS submitted_at,
    r.job_title AS technology
FROM user_details u 
JOIN requirements_model r ON r.assigned_by = u.user_name  
JOIN candidate_submissions cs ON cs.job_id = r.job_id    
JOIN candidates c ON c.candidate_id = cs.candidate_id   
WHERE u.user_id = :userId AND c.user_id != u.user_id   
AND cs.profile_received_date BETWEEN :startDate AND :endDate 
AND cs.job_id IN (SELECT r2.job_id FROM requirements_model r2 WHERE r2.assigned_by = u.user_name)""", nativeQuery = true)
	List<Tuple> findTeamSubmissionsByTeamleadAndDateRange(
			@Param("userId") String userId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate
	);
	@Query(value = """    
SELECT         
    cs.submission_id,        
    cs.recruiter_name as recruiter_name,       
    cs.candidate_id AS candidate_id,        
    c.full_name AS full_name,       
    c.contact_number AS contact_number,      
    c.candidate_email_id AS candidate_email_id,       
    cs.skills AS skills,        
    cs.job_id AS job_id,        
    cs.user_id AS user_id,  
    cs.user_email AS user_email,        
    cs.preferred_location AS preferred_location,   
    DATE_FORMAT(cs.profile_received_date, '%Y-%m-%d') AS profile_received_date,   
    r.job_title AS job_title,       
    r.client_name AS client_name,    
    c.total_experience AS total_experience,
    c.relevant_experience AS relevant_experience,
    c.current_organization AS current_organization,
    c.qualification AS qualification,
    c.currentctc AS current_ctc,
    c.expectedctc AS expected_ctc,
    c.notice_period AS notice_period,
    c.current_location AS current_location,
    cs.communication_skills AS communication_skills,
    cs.required_technologies_rating AS required_technologies_rating,
    cs.overall_feedback AS overall_feedback,
    cs.submitted_at AS submitted_at,
    r.job_title AS technology
FROM candidates c     
JOIN candidate_submissions cs ON c.candidate_id = cs.candidate_id  
JOIN requirements_model r ON cs.job_id = r.job_id    
WHERE cs.user_id = :userId     
AND cs.profile_received_date BETWEEN :startDate AND :endDate""", nativeQuery = true)
	List<Tuple> findSelfSubmissionsByTeamleadAndDateRange(
			@Param("userId") String userId,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate
	);
    @Modifying
    @Transactional
    @Query(value = "UPDATE requirements_model r SET r.status = 'Submitted' " +
            "WHERE r.job_id = :jobId AND EXISTS " +
            "(SELECT 1 FROM candidate_submissions c WHERE c.job_id = :jobId)", nativeQuery = true)
    void updateRequirementStatus(@Param("jobId") String jobId);

    List<Submissions> findByJobId(String jobId);
}
