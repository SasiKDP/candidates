package com.profile.candidate.service;

import com.profile.candidate.dto.PlacementDto;
import com.profile.candidate.dto.PlacementResponseDto;
import com.profile.candidate.exceptions.CandidateAlreadyExistsException;
import com.profile.candidate.exceptions.DuplicateInterviewPlacementException;
import com.profile.candidate.exceptions.InvalidRateException;
import com.profile.candidate.exceptions.ResourceNotFoundException;
import com.profile.candidate.model.InterviewDetails;
import com.profile.candidate.model.PlacementDetails;
import com.profile.candidate.repository.InterviewRepository;
import com.profile.candidate.repository.PlacementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlacementService {

    private static final Logger logger = LoggerFactory.getLogger(PlacementService.class);


    private final PlacementRepository placementRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public PlacementService(PlacementRepository placementRepository) {
        this.placementRepository = placementRepository;
    }

    @Autowired
    private  InterviewRepository interviewRepository;

    private String generateCustomId() {
        List<Integer> existingNumbers = placementRepository.findAll().stream()
                .map(PlacementDetails::getId)
                .filter(id -> id != null && id.matches("PLMNT\\d{4}"))
                .map(id -> Integer.parseInt(id.replace("PLMNT", "")))
                .toList();

        int nextNumber = existingNumbers.stream().max(Integer::compare).orElse(0) + 1;
        return String.format("PLMNT%04d", nextNumber);
    }

    public PlacementResponseDto savePlacement(PlacementDto placementDto) {
        PlacementDetails placementDetails = convertToEntity(placementDto);

        // Validate payRate < billRate
        if (placementDetails.getPayRate() != null && placementDetails.getBillRate() != null) {
            if (placementDetails.getPayRate().compareTo(placementDetails.getBillRate()) > 0) {
                throw new InvalidRateException("Pay Rate cannot be greater than Bill Rate.");
            }
        }

        placementDetails.setId(generateCustomId());
        logger.info("Generated ID is: " + placementDetails.getId());

        // Calculate Gross Profit
        if (placementDetails.getBillRate() != null && placementDetails.getPayRate() != null) {
            BigDecimal grossProfit = placementDetails.getBillRate()
                    .subtract(placementDetails.getPayRate())
                    .setScale(2, RoundingMode.HALF_UP);
            placementDetails.setGrossProfit(grossProfit);
        }

        // Check for duplicate interview ID
        if (placementDto.getInterviewId() != null) {
            boolean alreadyPlaced = placementRepository.existsByInterviewId(placementDto.getInterviewId());
            if (alreadyPlaced) {
                throw new DuplicateInterviewPlacementException("Interview ID " + placementDto.getInterviewId() + " is already used in a placement.");
            }

            // Update interviewDetails.isPlaced = true
            Optional<InterviewDetails> interviewDetailsOpt = interviewRepository.findById(placementDto.getInterviewId());
            if (interviewDetailsOpt.isPresent()) {
                InterviewDetails interviewDetails = interviewDetailsOpt.get();
                interviewDetails.setIsPlaced(true);
                interviewRepository.save(interviewDetails);
                logger.info("Interview details updated: " + interviewDetails);
            } else {
                logger.warn("Interview ID " + placementDto.getInterviewId() + " not found. Proceeding without updating interview details.");
            }

        } else {
            logger.info("No interview ID provided. Skipping interview details update.");
        }

        placementDetails.setStatus("Active");
        PlacementDetails saved = placementRepository.save(placementDetails);
        boolean isPlaced = "Active".equalsIgnoreCase(saved.getStatus());

        return new PlacementResponseDto(
                saved.getId(),
                saved.getCandidateFullName(),
                saved.getCandidateContactNo(),
                isPlaced
        );
    }



    public PlacementResponseDto updatePlacement(String id, PlacementDto dto) {
        PlacementDetails existing = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found with ID: " + id));

        if (dto.getCandidateEmailId()!= null && !dto.getCandidateEmailId().equals(existing.getCandidateEmailId())) {
            existing.setCandidateEmailId(dto.getCandidateEmailId());
        }

        if (dto.getCandidateContactNo() != null && !dto.getCandidateContactNo().equals(existing.getCandidateContactNo())) {
            existing.setCandidateContactNo(dto.getCandidateContactNo());
        }

        Optional.ofNullable(dto.getCandidateFullName()).ifPresent(existing::setCandidateFullName);
        Optional.ofNullable(dto.getTechnology()).ifPresent(existing::setTechnology);
        Optional.ofNullable(dto.getClientName()).ifPresent(existing::setClientName);
        Optional.ofNullable(dto.getVendorName()).ifPresent(existing::setVendorName);
        Optional.ofNullable(dto.getStartDate()).ifPresent(start ->
                existing.setStartDate(LocalDate.parse(start, formatter)));
        Optional.ofNullable(dto.getEndDate()).ifPresent(end ->
                existing.setEndDate(LocalDate.parse(end, formatter)));
        Optional.ofNullable(dto.getRecruiter()).ifPresent(existing::setRecruiter);
        Optional.ofNullable(dto.getSales()).ifPresent(existing::setSales);
        Optional.ofNullable(dto.getEmploymentType()).ifPresent(existing::setEmploymentType);
        Optional.ofNullable(dto.getRemarks()).ifPresent(existing::setRemarks);
        Optional.ofNullable(dto.getStatus()).ifPresent(existing::setStatus);
        Optional.ofNullable(dto.getStatusMessage()).ifPresent(existing::setStatusMessage);

        if (dto.getPayRate() != null) {
            existing.setPayRate(dto.getPayRate());
        }

        // 🔥 Important: update billRate if available
        if (dto.getBillRate() != null) {
            existing.setBillRate(dto.getBillRate());
        }

        // 🔥 Recalculate grossProfit if both payRate and billRateINR are present
        if (existing.getBillRate() != null && existing.getPayRate() != null) {
            BigDecimal grossProfit = existing.getBillRate()
                    .subtract(existing.getPayRate())
                    .setScale(2, RoundingMode.HALF_UP);
            existing.setGrossProfit(grossProfit);
        }

        PlacementDetails updated = placementRepository.save(existing);
        return convertToResponseDto(updated);

    }

    public void deletePlacement(String id) {
        if (!placementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Placement not found with ID: " + id);
        }
        placementRepository.deleteById(id);
    }

    // ✅ UPDATED: Return full placement details using PlacementDto
    public List<PlacementDetails> getAllPlacements() {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1); // 1st of current month
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth()); // last day of current month
        return placementRepository.findPlacementsByCreatedAtBetween(startDate, endDate);// Fetch directly from PlacementDetails table
    }


    public PlacementResponseDto getPlacementById(String id) {
        PlacementDetails placement = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found with ID: " + id));
        return convertToResponseDto(placement);
    }



    private PlacementResponseDto convertToResponseDto(PlacementDetails updated) {
        return new PlacementResponseDto(
                updated.getId(),
                updated.getCandidateFullName(),
                updated.getCandidateContactNo()

        );
    }

    private PlacementDetails convertToEntity(PlacementDto dto) {
        PlacementDetails entity = new PlacementDetails();
        entity.setCandidateFullName(dto.getCandidateFullName());
        entity.setCandidateContactNo(dto.getCandidateContactNo());
        entity.setCandidateEmailId(dto.getCandidateEmailId());
        entity.setCandidateId(dto.getCandidateId());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setTechnology(dto.getTechnology());
        entity.setClientName(dto.getClientName());
        entity.setVendorName(dto.getVendorName());
        entity.setStartDate(dto.getStartDate() != null ? LocalDate.parse(dto.getStartDate(), formatter) : null);
        entity.setEndDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate(), formatter) : null);
        entity.setRecruiter(dto.getRecruiter());
        entity.setSales(dto.getSales());
        entity.setPayRate(dto.getPayRate());
        entity.setBillRate(dto.getBillRate());
        entity.setEmploymentType(dto.getEmploymentType());
        entity.setRemarks(dto.getRemarks());
        entity.setStatus(dto.getStatus());
        entity.setStatusMessage(dto.getStatusMessage());
        entity.setInterviewId(dto.getInterviewId());
        return entity;
    }


    public Map<String, Long> getCounts() {
        Object[] result = (Object[]) placementRepository.getAllCounts();

        Map<String, Long> counts = new HashMap<>();
        counts.put("requirements", ((Number) result[0]).longValue());
        counts.put("candidates", ((Number) result[1]).longValue());
        counts.put("clients", ((Number) result[2]).longValue());
        counts.put("placements", ((Number) result[3]).longValue());
        counts.put("bench", ((Number) result[4]).longValue());
        counts.put("users", ((Number) result[5]).longValue());
        counts.put("interviews", ((Number) result[6]).longValue());

        return counts;
    }

    public List<PlacementDetails> getPlacementsByDateRange(LocalDate startDate, LocalDate endDate) {
        return placementRepository.findPlacementsByCreatedAtBetween(startDate, endDate);
    }
}
