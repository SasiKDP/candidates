package com.profile.candidate.service;

import com.profile.candidate.dto.InterviewDto;
import com.profile.candidate.dto.PlacementDto;
import com.profile.candidate.dto.PlacementResponseDto;
import com.profile.candidate.exceptions.InvalidRateException;
import com.profile.candidate.exceptions.ResourceNotFoundException;
import com.profile.candidate.model.PlacementDetails;
import com.profile.candidate.repository.PlacementRepository;
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

    private final PlacementRepository placementRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public PlacementService(PlacementRepository placementRepository) {
        this.placementRepository = placementRepository;
    }

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

        if (placementRepository.existsByPhone(placementDetails.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + placementDetails.getPhone());
        }

        if (placementRepository.existsByConsultantEmail(placementDetails.getConsultantEmail())) {
            throw new IllegalArgumentException("Email already exists: " + placementDetails.getConsultantEmail());
        }

        if (placementDetails.getPayRate() != null && placementDetails.getBillRateUSD() != null) {
            if (placementDetails.getPayRate().compareTo(placementDetails.getBillRateUSD()) > 0) {
                throw new InvalidRateException("Pay Rate cannot be greater than Bill Rate USD.");
            }
        }

        placementDetails.setId(generateCustomId());

        if (placementDetails.getBillRateUSD() != null) {
            BigDecimal billRateINR = placementDetails.getBillRateUSD()
                    .multiply(BigDecimal.valueOf(83))
                    .setScale(2, RoundingMode.HALF_UP);
            placementDetails.setBillRateINR(billRateINR);
        }

        if (placementDetails.getBillRateUSD() != null && placementDetails.getPayRate() != null) {
            BigDecimal grossProfit = placementDetails.getBillRateUSD()
                    .subtract(placementDetails.getPayRate())
                    .setScale(2, RoundingMode.HALF_UP);
            placementDetails.setGrossProfit(grossProfit);
        }

        PlacementDetails saved = placementRepository.save(placementDetails);
        return convertToResponseDto(saved);
    }

    public PlacementResponseDto updatePlacement(String id, PlacementDto dto) {
        PlacementDetails existing = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found with ID: " + id));

        if (dto.getConsultantEmail() != null && !dto.getConsultantEmail().equals(existing.getConsultantEmail())) {
            if (placementRepository.existsByConsultantEmail(dto.getConsultantEmail())) {
                throw new IllegalArgumentException("Email already exists: " + dto.getConsultantEmail());
            }
            existing.setConsultantEmail(dto.getConsultantEmail());
        }

        if (dto.getPhone() != null && !dto.getPhone().equals(existing.getPhone())) {
            if (placementRepository.existsByPhone(dto.getPhone())) {
                throw new IllegalArgumentException("Phone number already exists: " + dto.getPhone());
            }
            existing.setPhone(dto.getPhone());
        }

        Optional.ofNullable(dto.getConsultantName()).ifPresent(existing::setConsultantName);
        Optional.ofNullable(dto.getTechnology()).ifPresent(existing::setTechnology);
        Optional.ofNullable(dto.getClient()).ifPresent(existing::setClient);
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

        if (dto.getBillRateUSD() != null) {
            existing.setBillRateUSD(dto.getBillRateUSD());
            BigDecimal billRateINR = dto.getBillRateUSD().multiply(BigDecimal.valueOf(83)).setScale(2, RoundingMode.HALF_UP);
            existing.setBillRateINR(billRateINR);
        }

        if (dto.getPayRate() != null) {
            existing.setPayRate(dto.getPayRate());
        }

        if (existing.getPayRate() != null && existing.getBillRateUSD() != null) {
            if (existing.getPayRate().compareTo(existing.getBillRateUSD()) > 0) {
                throw new InvalidRateException("Pay Rate cannot be greater than Bill Rate USD.");
            }
            BigDecimal grossProfit = existing.getBillRateUSD().subtract(existing.getPayRate()).setScale(2, RoundingMode.HALF_UP);
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
    public List<PlacementDto> getAllPlacements() {
        return placementRepository.findAll().stream()
                .map(this::convertToDto) // full DTO with all fields
                .collect(Collectors.toList());
    }

    public PlacementResponseDto getPlacementById(String id) {
        PlacementDetails placement = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found with ID: " + id));
        return convertToResponseDto(placement);
    }

    public void autoAddPlacementFromInterview(InterviewDto interviewDto) {
        if ("Placed".equalsIgnoreCase(interviewDto.getInterviewStatus())) {
            PlacementDetails placement = new PlacementDetails();
            placement.setId(generateCustomId());
            placement.setConsultantName(interviewDto.getFullName());
            placement.setConsultantEmail(interviewDto.getUserEmail());
            placement.setPhone(interviewDto.getContactNumber());
            placement.setTechnology("N/A");
            placement.setStartDate(LocalDate.now());
            placement.setRecruiter(interviewDto.getUserId());
            placement.setClient(interviewDto.getClientName());
            placement.setEmploymentType("C2C");
            placement.setStatus("Running");

            placementRepository.save(placement);
        }
    }

    private PlacementDto convertToDto(PlacementDetails saved) {
        PlacementDto dto = new PlacementDto();
        dto.setId(saved.getId());
        dto.setConsultantName(saved.getConsultantName());
        dto.setPhone(saved.getPhone());
        dto.setConsultantEmail(saved.getConsultantEmail());
        dto.setTechnology(saved.getTechnology());
        dto.setClient(saved.getClient());
        dto.setVendorName(saved.getVendorName());
        dto.setStartDate(saved.getStartDate() != null ? saved.getStartDate().toString() : null);
        dto.setEndDate(saved.getEndDate() != null ? saved.getEndDate().toString() : null);
        dto.setRecruiter(saved.getRecruiter());
        dto.setSales(saved.getSales());
        dto.setBillRateUSD(saved.getBillRateUSD());
        dto.setBillRateINR(saved.getBillRateINR());
        dto.setPayRate(saved.getPayRate());
        dto.setGrossProfit(saved.getGrossProfit());
        dto.setEmploymentType(saved.getEmploymentType());
        dto.setRemarks(saved.getRemarks());
        dto.setStatus(saved.getStatus());
        dto.setStatusMessage(saved.getStatusMessage());
        return dto;
    }

    private PlacementResponseDto convertToResponseDto(PlacementDetails saved) {
        return new PlacementResponseDto(
                saved.getId(),
                saved.getConsultantName(),
                saved.getPhone(),
                saved.getConsultantEmail()
        );
    }

    private PlacementDetails convertToEntity(PlacementDto dto) {
        PlacementDetails entity = new PlacementDetails();
        entity.setConsultantName(dto.getConsultantName());
        entity.setPhone(dto.getPhone());
        entity.setConsultantEmail(dto.getConsultantEmail());
        entity.setTechnology(dto.getTechnology());
        entity.setClient(dto.getClient());
        entity.setVendorName(dto.getVendorName());
        entity.setStartDate(dto.getStartDate() != null ? LocalDate.parse(dto.getStartDate(), formatter) : null);
        entity.setEndDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate(), formatter) : null);
        entity.setRecruiter(dto.getRecruiter());
        entity.setSales(dto.getSales());
        entity.setBillRateUSD(dto.getBillRateUSD());
        entity.setPayRate(dto.getPayRate());
        entity.setEmploymentType(dto.getEmploymentType());
        entity.setRemarks(dto.getRemarks());
        entity.setStatus(dto.getStatus());
        entity.setStatusMessage(dto.getStatusMessage());
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
}
