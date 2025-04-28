package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submissions,String> {


     List<Submissions> findByCandidate_CandidateId(String candidateId);

     Submissions findByCandidate_CandidateIdAndJobId(String candidateId, String jobId);



    List<Submissions> findByCandidate_CandidateIdIn(List<String> candidateIds);

     List<Submissions> findByCandidate(CandidateDetails candidate);

    Submissions findByCandidate_ContactNumberAndJobId(String contactNumber, String jobId);

    Submissions findByCandidate_CandidateEmailIdAndJobId(String candidateId, String jobId);

    @Query("SELECT s.candidate.candidateId FROM Submissions s WHERE s.submissionId = :submissionId")
    String findCandidateIdBySubmissionId(@Param("submissionId") String submissionId);

}
