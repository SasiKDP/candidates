package com.profile.candidate.repository;

import com.profile.candidate.model.CandidateDetails;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<CandidateDetails, String> {
    // Additional custom queries if needed
    // Find candidate by email

    Optional<CandidateDetails> findByFullName(String fullName);

    Optional<CandidateDetails> findByCandidateEmailId(String candidateEmailId);

    Optional<CandidateDetails> findByContactNumber(String contactNumber);

    List<CandidateDetails> findByUserIdAndJobId(String userId, String jobId);

    List<CandidateDetails> findByJobId(String jobId);


    // Find all candidates with specific total experience
    List<CandidateDetails> findByTotalExperience(Integer totalExperience);

    // Find all candidates with specific skills (if skills are a list)
    List<CandidateDetails> findBySkillsContaining(String skill);

    // Find candidates by notice period
    List<CandidateDetails> findByNoticePeriod(String noticePeriod);

    // Fetch candidate by candidateId
    Optional<CandidateDetails> findByCandidateId(String candidateId);


    Optional<CandidateDetails> findByFullNameAndCandidateEmailIdAndContactNumber(String fullName, String candidateEmailId, String contactNumber);

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

    // Check if an interview is already scheduled for a candidate with the same jobId and interviewDateTime
    boolean existsByCandidateIdAndJobIdAndInterviewDateTime(String candidateId,
                                                            String jobId,
                                                            OffsetDateTime interviewDateTime);

    @Modifying
    @Transactional
    @Query(value = "UPDATE requirements_model r SET r.status = 'Submitted' " +
            "WHERE r.job_id = :jobId AND EXISTS " +
            "(SELECT 1 FROM candidates c WHERE c.job_id = :jobId)", nativeQuery = true)
    void updateRequirementStatus(@Param("jobId") String jobId);
}