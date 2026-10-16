import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  COMPLAINT_CATEGORY_LABELS,
  COMPLAINT_STATUSES,
  COMPLAINT_STATUS_LABELS,
  Complaint,
  ComplaintStatus
} from '../../../core/models/complaint.model';
import { AuthService } from '../../../core/services/auth.service';
import { ComplaintService } from '../../../core/services/complaint.service';

@Component({
  selector: 'app-complaint-detail',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './complaint-detail.html',
  styleUrl: './complaint-detail.css'
})
export class ComplaintDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly complaintService = inject(ComplaintService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly categoryLabels = COMPLAINT_CATEGORY_LABELS;
  protected readonly statusLabels = COMPLAINT_STATUS_LABELS;
  protected readonly statuses = COMPLAINT_STATUSES;
  protected readonly isAdmin = this.authService.isAdmin;

  protected readonly complaint = signal<Complaint | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly deleting = signal(false);

  protected readonly statusForm = this.formBuilder.nonNullable.group({
    status: this.formBuilder.nonNullable.control<ComplaintStatus>('EN_COURS', Validators.required),
    adminResponse: ['']
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
  }

  protected canDelete(): boolean {
    const complaint = this.complaint();
    const user = this.authService.currentUser();
    if (!complaint || !user) {
      return false;
    }
    if (user.role === 'ADMIN') {
      return true;
    }
    return user.id === complaint.author.id && complaint.status === 'OUVERTE';
  }

  protected saveStatus(): void {
    const complaint = this.complaint();
    if (!complaint || this.statusForm.invalid) {
      this.statusForm.markAllAsTouched();
      return;
    }

    const raw = this.statusForm.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.complaintService.updateStatus(complaint.id, {
      status: raw.status,
      adminResponse: raw.adminResponse || null
    }).subscribe({
      next: (updated) => {
        this.complaint.set(updated);
        this.submitting.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La mise a jour a echoue'));
        this.submitting.set(false);
      }
    });
  }

  protected remove(): void {
    const complaint = this.complaint();
    if (!complaint) {
      return;
    }
    if (!confirm(`Supprimer la reclamation « ${complaint.subject} » ?`)) {
      return;
    }
    this.deleting.set(true);
    this.complaintService.delete(complaint.id).subscribe({
      next: () => this.router.navigate(['/reclamations']),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La suppression a echoue'));
        this.deleting.set(false);
      }
    });
  }

  private load(id: number): void {
    this.complaintService.get(id).subscribe({
      next: (complaint) => {
        this.complaint.set(complaint);
        this.statusForm.patchValue({
          status: complaint.status,
          adminResponse: complaint.adminResponse ?? ''
        });
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Reclamation introuvable'))
    });
  }
}
