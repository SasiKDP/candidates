package com.profile.candidate.service;

import com.profile.candidate.dto.DeleteSubmissionResponseDto;
import com.profile.candidate.dto.SubmissionsGetResponse;
import com.profile.candidate.exceptions.CandidateNotFoundException;
import com.profile.candidate.exceptions.SubmissionNotFoundException;
import com.profile.candidate.model.CandidateDetails;
import com.profile.candidate.model.Submissions;
import com.profile.candidate.repository.CandidateRepository;
import com.profile.candidate.repository.SubmissionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    @Autowired
    SubmissionRepository submissionRepository;
    @Autowired
    CandidateRepository candidateRepository;

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    public List<SubmissionsGetResponse> getAllSubmissions() {
        List<Submissions> submissions = submissionRepository.findAll();
        return submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
    }

    public List<SubmissionsGetResponse> getSubmissions(String candidateId) {
        Optional<CandidateDetails> candidateDetails = candidateRepository.findById(candidateId);
        if (candidateDetails.isEmpty()) {
            throw new CandidateNotFoundException("Invalid CandidateId " + candidateId);
        }

        List<Submissions> submissions = submissionRepository.findByCandidate_CandidateId(candidateId);
        return submissions.stream()
                .map(this::convertToSubmissionsGetResponse)
                .collect(Collectors.toList());
    }
    public SubmissionsGetResponse getSubmissionById(String submissionId) {
        Optional<Submissions> submissions = submissionRepository.findById(submissionId);

        if (submissions.isEmpty()) {
            throw new SubmissionNotFoundException("Invalid SubmissionId " + submissionId);
        }
        return convertToSubmissionsGetResponse(submissions.get());
    }

    private SubmissionsGetResponse convertToSubmissionsGetResponse(Submissions sub) {
        SubmissionsGetResponse dto = new SubmissionsGetResponse();

        dto.setSubmissionId(sub.getSubmissionId());
        dto.setCandidateId(sub.getCandidate().getCandidateId());
        dto.setJobId(sub.getJobId());
        dto.setSubmittedAt(sub.getSubmittedAt());
        dto.setCommunicationSkills(sub.getCommunicationSkills());
        dto.setSkills(sub.getSkills());
        dto.setOverallFeedback(sub.getOverallFeedback());
        dto.setPreferredLocation(sub.getPreferredLocation());
        CandidateDetails candidate = sub.getCandidate();
        //CandidateDto candidateDto = new CandidateDto();

        dto.setUserId(candidate.getUserId());
        dto.setFullName(candidate.getFullName());
        dto.setCandidateEmailId(candidate.getCandidateEmailId());
        dto.setContactNumber(candidate.getContactNumber());
        dto.setCurrentOrganization(candidate.getCurrentOrganization());
        dto.setQualification(candidate.getQualification());
        dto.setTotalExperience(candidate.getTotalExperience());
        dto.setRelevantExperience(candidate.getRelevantExperience());
        dto.setCurrentCTC(candidate.getCurrentCTC());
        dto.setExpectedCTC(candidate.getExpectedCTC());
        dto.setNoticePeriod(candidate.getNoticePeriod());
        dto.setCurrentLocation(candidate.getCurrentLocation());

        return dto;
    }
    public List<SubmissionsGetResponse> getSubmissionsByUserId(String userId) {

        List<CandidateDetails> candidates = candidateRepository.findByUserId(userId);
        if (candidates.isEmpty()) throw new CandidateNotFoundException("No Candidate Found for UserId: "+userId);
        List<SubmissionsGetResponse> submissionsResponses = new ArrayList<>();

        for (CandidateDetails candidate : candidates) {
            List<Submissions> submissions =  submissionRepository.findByCandidate(candidate);

            for (Submissions submission : submissions) {
                submissionsResponses.add(convertToSubmissionsGetResponse(submission));
            }
        }
        return submissionsResponses;
    }
    @Transactional
    public DeleteSubmissionResponseDto deleteSubmissionById(String submissionId) {
        logger.info("Received request to delete candidate with candidateId: {}", submissionId);
        // Fetch candidate details before deletion
        Submissions submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> {
                    logger.error("Submission with ID {} not found", submissionId);
                    return new SubmissionNotFoundException("Submission not found with id: " + submissionId);
                });
        logger.info("Candidate found: {}, Proceeding with deletion", submission.getSubmissionId());
        // Store the candidate details before deletion
        String submissionIdBeforeDelete = submission.getSubmissionId();
        String jobIdBeforeDelete = submission.getJobId();

        // Delete the candidate from the repository
        submissionRepository.delete(submission);
        logger.info("Candidate with ID {} deleted successfully", submissionId);

        // Prepare the response with candidate details
        DeleteSubmissionResponseDto.Payload payload = new DeleteSubmissionResponseDto.Payload(submissionIdBeforeDelete, jobIdBeforeDelete);

        return new DeleteSubmissionResponseDto(true,
                "Submission deleted successfully",
                payload,
                null);
    }

}


