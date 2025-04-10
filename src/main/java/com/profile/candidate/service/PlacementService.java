package com.profile.candidate.service;

import com.profile.candidate.dto.InterviewDto;
import com.profile.candidate.exceptions.InvalidRateException;
import com.profile.candidate.exceptions.ResourceNotFoundException;
import com.profile.candidate.model.PlacementDetails;
import com.profile.candidate.repository.PlacementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PlacementService {

    private final PlacementRepository placementRepository;

    @Autowired
    public PlacementService(PlacementRepository placementRepository) {
        this.placementRepository = placementRepository;
    }

    // Generate Custom ID like PLACEMENT001
    private String generateCustomId() {
        List<Integer> existingNumbers = placementRepository.findAll().stream()
                .map(PlacementDetails::getId)
                .filter(id -> id != null && id.matches("PLMNT\\d{4}"))
                .map(id -> Integer.parseInt(id.replace("PLMNT", "")))
                .toList();

        int nextNumber = existingNumbers.stream().max(Integer::compare).orElse(0) + 1;
        return String.format("PLMNT%04d", nextNumber);
    }

    // Save Placement with validation
    public PlacementDetails savePlacement(PlacementDetails placementDetails) {
        // Unique validations
        if (placementRepository.existsByPhone(placementDetails.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + placementDetails.getPhone());
        }

        if (placementRepository.existsByConsultantEmail(placementDetails.getConsultantEmail())) {
            throw new IllegalArgumentException("Email already exists: " + placementDetails.getConsultantEmail());
        }

        // Validate payRate <= billRateUSD
        if (placementDetails.getPayRate() != null && placementDetails.getBillRateUSD() != null) {
            if (placementDetails.getPayRate().compareTo(placementDetails.getBillRateUSD()) > 0) {
                throw new InvalidRateException("Pay Rate cannot be greater than Bill Rate USD.");
            }
        }

        placementDetails.setId(generateCustomId());

        // Calculate INR and Gross Profit
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

        return placementRepository.save(placementDetails);
    }

    // Update placement
    public PlacementDetails updatePlacement(String id, PlacementDetails updatedPlacement) {
        PlacementDetails existingPlacement = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found with ID: " + id));

        // Email uniqueness
        Optional.ofNullable(updatedPlacement.getConsultantEmail()).ifPresent(email -> {
            if (!email.equals(existingPlacement.getConsultantEmail())
                    && placementRepository.existsByConsultantEmail(email)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }
            existingPlacement.setConsultantEmail(email);
        });

        // Phone uniqueness
        Optional.ofNullable(updatedPlacement.getPhone()).ifPresent(phone -> {
            if (!phone.equals(existingPlacement.getPhone())
                    && placementRepository.existsByPhone(phone)) {
                throw new IllegalArgumentException("Phone number already exists: " + phone);
            }
            existingPlacement.setPhone(phone);
        });

        // Update fields
        Optional.ofNullable(updatedPlacement.getConsultantName()).ifPresent(existingPlacement::setConsultantName);
        Optional.ofNullable(updatedPlacement.getTechnology()).ifPresent(existingPlacement::setTechnology);
        Optional.ofNullable(updatedPlacement.getClient()).ifPresent(existingPlacement::setClient);
        Optional.ofNullable(updatedPlacement.getVendorName()).ifPresent(existingPlacement::setVendorName);
        Optional.ofNullable(updatedPlacement.getStartDate()).ifPresent(existingPlacement::setStartDate);
        Optional.ofNullable(updatedPlacement.getEndDate()).ifPresent(existingPlacement::setEndDate);
        Optional.ofNullable(updatedPlacement.getRecruiter()).ifPresent(existingPlacement::setRecruiter);
        Optional.ofNullable(updatedPlacement.getSales()).ifPresent(existingPlacement::setSales);
        Optional.ofNullable(updatedPlacement.getEmploymentType()).ifPresent(existingPlacement::setEmploymentType);
        Optional.ofNullable(updatedPlacement.getRemarks()).ifPresent(existingPlacement::setRemarks);
        Optional.ofNullable(updatedPlacement.getStatus()).ifPresent(existingPlacement::setStatus);
        Optional.ofNullable(updatedPlacement.getStatusMessage()).ifPresent(existingPlacement::setStatusMessage);

        // Bill Rate and Pay Rate updates
        if (updatedPlacement.getBillRateUSD() != null) {
            existingPlacement.setBillRateUSD(updatedPlacement.getBillRateUSD());
            BigDecimal billRateINR = updatedPlacement.getBillRateUSD()
                    .multiply(BigDecimal.valueOf(83))
                    .setScale(2, RoundingMode.HALF_UP);
            existingPlacement.setBillRateINR(billRateINR);
        }

        if (updatedPlacement.getPayRate() != null) {
            existingPlacement.setPayRate(updatedPlacement.getPayRate());
        }

        // Validate payRate <= billRateUSD again during update
        if (existingPlacement.getPayRate() != null && existingPlacement.getBillRateUSD() != null) {
            if (existingPlacement.getPayRate().compareTo(existingPlacement.getBillRateUSD()) > 0) {
                throw new InvalidRateException("Pay Rate cannot be greater than Bill Rate USD.");
            }

            // Recalculate gross profit
            BigDecimal grossProfit = existingPlacement.getBillRateUSD()
                    .subtract(existingPlacement.getPayRate())
                    .setScale(2, RoundingMode.HALF_UP);
            existingPlacement.setGrossProfit(grossProfit);
        }

        return placementRepository.save(existingPlacement);
    }

    // Delete Placement
    public void deletePlacement(String id) {
        if (!placementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Placement not found with ID: " + id);
        }
        placementRepository.deleteById(id);
    }

    // Get all
    public List<PlacementDetails> getAllPlacements() {
        return placementRepository.findAll();
    }
    //get by ID
    public PlacementDetails getPlacementById(String id) {
        return placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found with ID: " + id));
    }


    // Auto-add placement from interview
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
}
