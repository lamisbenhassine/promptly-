package com.promptly.backend.recruitment;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.recruitment.dto.ApplicationStatusRequest;
import com.promptly.backend.recruitment.dto.JobApplicationResponse;
import com.promptly.backend.recruitment.dto.JobOfferRequest;
import com.promptly.backend.recruitment.dto.JobOfferResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recruitment")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @GetMapping("/offers")
    public List<JobOfferResponse> listOffers(Principal principal) {
        return recruitmentService.listOffers(principal.getName());
    }

    @GetMapping("/offers/{id}")
    public JobOfferResponse getOffer(Principal principal, @PathVariable Long id) {
        return recruitmentService.getOffer(id, principal.getName());
    }

    @PostMapping("/offers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JobOfferResponse> createOffer(Principal principal,
                                                        @Valid @RequestBody JobOfferRequest request) {
        JobOfferResponse created = recruitmentService.createOffer(principal.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/recruitment/offers/" + created.id()))
                .body(created);
    }

    @PutMapping("/offers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public JobOfferResponse updateOffer(Principal principal,
                                        @PathVariable Long id,
                                        @Valid @RequestBody JobOfferRequest request) {
        return recruitmentService.updateOffer(id, principal.getName(), request);
    }

    @DeleteMapping("/offers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOffer(Principal principal, @PathVariable Long id) {
        recruitmentService.deleteOffer(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/offers/{id}/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JobApplicationResponse apply(Principal principal,
                                        @PathVariable Long id,
                                        @RequestPart("cv") MultipartFile cv,
                                        @RequestParam(value = "message", required = false) String message) {
        return recruitmentService.apply(id, principal.getName(), cv, message);
    }

    @GetMapping("/offers/{id}/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public List<JobApplicationResponse> listApplications(Principal principal, @PathVariable Long id) {
        return recruitmentService.listApplicationsForOffer(id, principal.getName());
    }

    @GetMapping("/applications/me")
    public List<JobApplicationResponse> myApplications(Principal principal) {
        return recruitmentService.listMyApplications(principal.getName());
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public JobApplicationResponse updateStatus(Principal principal,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ApplicationStatusRequest request) {
        return recruitmentService.updateApplicationStatus(id, principal.getName(), request);
    }
}
