import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

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
  selector: 'app-complaint-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './complaint-list.html',
  styleUrl: './complaint-list.css'
})
export class ComplaintList implements OnInit {

  private readonly complaintService = inject(ComplaintService);
  private readonly authService = inject(AuthService);

  protected readonly statuses = COMPLAINT_STATUSES;
  protected readonly statusLabels = COMPLAINT_STATUS_LABELS;
  protected readonly categoryLabels = COMPLAINT_CATEGORY_LABELS;
  protected readonly isAdmin = this.authService.isAdmin;

  protected readonly complaints = signal<Complaint[]>([]);
  protected readonly selectedStatus = signal<ComplaintStatus | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  protected filterBy(status: ComplaintStatus | null): void {
    this.selectedStatus.set(status);
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.complaintService.list({ status: this.selectedStatus() }).subscribe({
      next: (page) => {
        this.complaints.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de charger les reclamations'));
        this.loading.set(false);
      }
    });
  }
}
