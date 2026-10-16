package com.promptly.backend.recruitment;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.promptly.backend.ai.PdfTextExtractor;
import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.recruitment.dto.ApplicationStatusRequest;
import com.promptly.backend.recruitment.dto.JobApplicationResponse;
import com.promptly.backend.recruitment.dto.JobOfferRequest;
import com.promptly.backend.recruitment.dto.JobOfferResponse;
import com.promptly.backend.storage.FileStorageService;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserService;

@Service
public class RecruitmentService {

    private final JobOfferRepository offerRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final CvMatchService cvMatchService;

    public RecruitmentService(JobOfferRepository offerRepository,
                              JobApplicationRepository applicationRepository,
                              UserService userService,
                              FileStorageService fileStorageService,
                              PdfTextExtractor pdfTextExtractor,
                              CvMatchService cvMatchService) {
        this.offerRepository = offerRepository;
        this.applicationRepository = applicationRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.cvMatchService = cvMatchService;
    }

    @Transactional(readOnly = true)
    public List<JobOfferResponse> listOffers(String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        boolean admin = viewer.getRole() == Role.ADMIN;
        List<JobOffer> offers = admin
                ? offerRepository.findAllByOrderByCreatedAtDesc()
                : offerRepository.findByPublishedTrueOrderByCreatedAtDesc();

        return offers.stream()
                .map(offer -> toOfferResponse(offer, viewer, admin))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getOffer(Long id, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        JobOffer offer = findOffer(id);
        boolean admin = viewer.getRole() == Role.ADMIN;
        if (!admin && !offer.isPublished()) {
            throw new ResourceNotFoundException("Offre", id);
        }
        return toOfferResponse(offer, viewer, admin);
    }

    @Transactional
    public JobOfferResponse createOffer(String adminEmail, JobOfferRequest request) {
        User admin = requireAdmin(adminEmail);
        JobOffer offer = new JobOffer(
                request.title().trim(),
                request.description().trim(),
                request.skills().trim(),
                blankToNull(request.experience()),
                blankToNull(request.location()),
                request.contractType(),
                request.published(),
                request.deadline(),
                admin);
        return toOfferResponse(offerRepository.save(offer), admin, true);
    }

    @Transactional
    public JobOfferResponse updateOffer(Long id, String adminEmail, JobOfferRequest request) {
        requireAdmin(adminEmail);
        JobOffer offer = findOffer(id);
        offer.setTitle(request.title().trim());
        offer.setDescription(request.description().trim());
        offer.setSkills(request.skills().trim());
        offer.setExperience(blankToNull(request.experience()));
        offer.setLocation(blankToNull(request.location()));
        offer.setContractType(request.contractType());
        offer.setPublished(request.published());
        offer.setDeadline(request.deadline());
        return toOfferResponse(offer, userService.findByEmail(adminEmail), true);
    }

    @Transactional
    public void deleteOffer(Long id, String adminEmail) {
        requireAdmin(adminEmail);
        JobOffer offer = findOffer(id);
        List<JobApplication> applications = applicationRepository.findByOfferIdOrderByCreatedAtDesc(id);
        applicationRepository.deleteAll(applications);
        offerRepository.delete(offer);
    }

    @Transactional
    public JobApplicationResponse apply(Long offerId, String applicantEmail,
                                        MultipartFile cv, String message) {
        User applicant = userService.findByEmail(applicantEmail);
        if (applicant.getRole() == Role.ADMIN) {
            throw new BusinessRuleException("Un administrateur ne peut pas postuler a une offre");
        }

        JobOffer offer = findOffer(offerId);
        if (!offer.isPublished()) {
            throw new BusinessRuleException("Cette offre n'est plus publiee");
        }
        if (offer.getDeadline() != null && offer.getDeadline().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("La date limite de candidature est depassee");
        }
        if (applicationRepository.existsByOfferIdAndApplicantId(offerId, applicant.getId())) {
            throw new BusinessRuleException("Vous avez deja postule a cette offre");
        }

        String cvPath = fileStorageService.store(
                cv, "cvs", FileStorageService.PDF_TYPES, 20L * 1024 * 1024);

        String note = message == null || message.isBlank() ? null : message.trim();
        JobApplication application = new JobApplication(offer, applicant, cvPath, note);

        try {
            String cvText = pdfTextExtractor.extract(fileStorageService.resolveExisting(cvPath));
            CvMatchResult match = cvMatchService.match(
                    offer.getSkills(), offer.getExperience(), cvText);
            application.setMatchScore(match.scorePercent());
            application.setMatchedSkills(CvMatchService.joinSkills(match.matchedSkills()));
            application.setMissingSkills(CvMatchService.joinSkills(match.missingSkills()));
            application.setExperienceMatched(match.experienceMatched());
            application.setMatchProvider(match.provider());
        } catch (RuntimeException e) {
            application.setMatchScore(0.0);
            application.setMatchProvider("error");
            application.setMatchedSkills(null);
            application.setMissingSkills(offer.getSkills());
            application.setExperienceMatched(false);
        }

        return JobApplicationResponse.from(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> listApplicationsForOffer(Long offerId, String adminEmail) {
        requireAdmin(adminEmail);
        findOffer(offerId);
        return applicationRepository.findByOfferIdOrderByCreatedAtDesc(offerId).stream()
                .map(JobApplicationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> listMyApplications(String applicantEmail) {
        User applicant = userService.findByEmail(applicantEmail);
        return applicationRepository.findByApplicantOrderByCreatedAtDesc(applicant).stream()
                .map(JobApplicationResponse::from)
                .toList();
    }

    @Transactional
    public JobApplicationResponse updateApplicationStatus(Long applicationId, String adminEmail,
                                                          ApplicationStatusRequest request) {
        requireAdmin(adminEmail);
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature", applicationId));
        application.setStatus(request.status());
        return JobApplicationResponse.from(application);
    }

    private JobOfferResponse toOfferResponse(JobOffer offer, User viewer, boolean admin) {
        long count = admin ? applicationRepository.countByOfferId(offer.getId()) : 0L;
        boolean applied = applicationRepository.existsByOfferIdAndApplicantId(
                offer.getId(), viewer.getId());
        return JobOfferResponse.from(offer, count, applied);
    }

    private JobOffer findOffer(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre", id));
    }

    private User requireAdmin(String email) {
        User user = userService.findByEmail(email);
        if (user.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Action reservee a l'administrateur");
        }
        return user;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
