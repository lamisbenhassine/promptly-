package com.promptly.backend.complaint;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.complaint.dto.ComplaintRequest;
import com.promptly.backend.complaint.dto.ComplaintResponse;
import com.promptly.backend.complaint.dto.ComplaintStatusRequest;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserService;

@Service
@Transactional(readOnly = true)
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserService userService;

    public ComplaintService(ComplaintRepository complaintRepository, UserService userService) {
        this.complaintRepository = complaintRepository;
        this.userService = userService;
    }

    public Page<ComplaintResponse> list(String viewerEmail, ComplaintStatus status, Pageable pageable) {
        User viewer = userService.findByEmail(viewerEmail);
        Long authorFilter = viewer.getRole() == Role.ADMIN ? null : viewer.getId();
        return complaintRepository.search(status, authorFilter, pageable)
                .map(ComplaintResponse::from);
    }

    public ComplaintResponse get(Long id, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        Complaint complaint = findById(id);
        ensureCanView(complaint, viewer);
        return ComplaintResponse.from(complaint);
    }

    @Transactional
    public ComplaintResponse create(String authorEmail, ComplaintRequest request) {
        User author = userService.findByEmail(authorEmail);
        Complaint complaint = new Complaint(
                request.subject().trim(),
                request.description().trim(),
                request.category(),
                author);
        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintResponse updateStatus(Long id, String adminEmail, ComplaintStatusRequest request) {
        User admin = userService.findByEmail(adminEmail);
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seul un administrateur peut traiter une reclamation.");
        }

        Complaint complaint = findById(id);
        ComplaintStatus newStatus = request.status();
        complaint.setStatus(newStatus);
        complaint.setHandledBy(admin);

        String response = request.adminResponse() == null ? null : request.adminResponse().trim();
        if (response != null && response.isBlank()) {
            response = null;
        }
        complaint.setAdminResponse(response);

        if (newStatus == ComplaintStatus.RESOLUE || newStatus == ComplaintStatus.REJETEE) {
            complaint.setResolvedAt(Instant.now());
            if (response == null) {
                throw new BusinessRuleException(
                        "Une reponse administrative est obligatoire pour clore une reclamation.");
            }
        } else {
            complaint.setResolvedAt(null);
        }

        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    @Transactional
    public void delete(Long id, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        Complaint complaint = findById(id);

        if (editor.getRole() == Role.ADMIN) {
            complaintRepository.delete(complaint);
            return;
        }

        if (!complaint.getAuthor().getId().equals(editor.getId())) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres reclamations.");
        }
        if (complaint.getStatus() != ComplaintStatus.OUVERTE) {
            throw new BusinessRuleException(
                    "Seule une reclamation encore ouverte peut etre supprimee par son auteur.");
        }
        complaintRepository.delete(complaint);
    }

    private Complaint findById(Long id) {
        return complaintRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamation", id));
    }

    private void ensureCanView(Complaint complaint, User viewer) {
        if (viewer.getRole() == Role.ADMIN
                || complaint.getAuthor().getId().equals(viewer.getId())) {
            return;
        }
        throw new ResourceNotFoundException("Reclamation", complaint.getId());
    }
}
