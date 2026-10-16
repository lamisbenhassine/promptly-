package com.promptly.backend.recruitment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.promptly.backend.user.User;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByOfferIdOrderByCreatedAtDesc(Long offerId);

    List<JobApplication> findByApplicantOrderByCreatedAtDesc(User applicant);

    Optional<JobApplication> findByOfferIdAndApplicantId(Long offerId, Long applicantId);

    boolean existsByOfferIdAndApplicantId(Long offerId, Long applicantId);

    long countByOfferId(Long offerId);
}
