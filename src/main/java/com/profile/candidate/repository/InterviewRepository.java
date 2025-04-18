package com.profile.candidate.repository;

import com.profile.candidate.model.InterviewDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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


}
