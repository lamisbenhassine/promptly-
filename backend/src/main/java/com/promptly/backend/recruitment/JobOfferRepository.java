package com.promptly.backend.recruitment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    List<JobOffer> findByPublishedTrueOrderByCreatedAtDesc();

    List<JobOffer> findAllByOrderByCreatedAtDesc();
}
