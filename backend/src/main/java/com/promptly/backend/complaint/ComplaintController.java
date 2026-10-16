package com.promptly.backend.complaint;

import java.net.URI;
import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

import com.promptly.backend.complaint.dto.ComplaintRequest;
import com.promptly.backend.complaint.dto.ComplaintResponse;
import com.promptly.backend.complaint.dto.ComplaintStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public Page<ComplaintResponse> list(
            Principal principal,
            @RequestParam(required = false) ComplaintStatus status,
            @PageableDefault(size = 12, sort = "updatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return complaintService.list(principal.getName(), status, pageable);
    }

    @GetMapping("/{id}")
    public ComplaintResponse get(Principal principal, @PathVariable Long id) {
        return complaintService.get(id, principal.getName());
    }

    @PostMapping
    public ResponseEntity<ComplaintResponse> create(Principal principal,
                                                    @Valid @RequestBody ComplaintRequest request) {
        ComplaintResponse created = complaintService.create(principal.getName(), request);
        return ResponseEntity
                .created(URI.create("/api/complaints/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintResponse updateStatus(Principal principal,
                                          @PathVariable Long id,
                                          @Valid @RequestBody ComplaintStatusRequest request) {
        return complaintService.updateStatus(id, principal.getName(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        complaintService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
