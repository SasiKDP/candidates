package com.profile.candidate.repository;

import com.profile.candidate.model.PlacementDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlacementRepository extends JpaRepository<PlacementDetails, String> {
    boolean existsByPhone(String phone);
    boolean existsByConsultantEmail(String consultantEmail);
}
