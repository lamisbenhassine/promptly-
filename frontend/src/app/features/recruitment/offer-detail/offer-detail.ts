import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  APPLICATION_STATUSES,
  APPLICATION_STATUS_LABELS,
  ApplicationStatus,
  CONTRACT_TYPE_LABELS,
  JobApplication,
  JobOffer
} from '../../../core/models/recruitment.model';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../../core/services/recruitment.service';

@Component({
  selector: 'app-offer-detail',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './offer-detail.html',
  styleUrl: './offer-detail.css'
})
export class OfferDetail implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly recruitmentService = inject(RecruitmentService);
  private readonly authService = inject(AuthService);

  protected readonly contractLabels = CONTRACT_TYPE_LABELS;
  protected readonly statusLabels = APPLICATION_STATUS_LABELS;
  protected readonly statuses = APPLICATION_STATUSES;
  protected readonly isAdmin = this.authService.isAdmin;
  protected readonly fileUrl = RecruitmentService.fileUrl;

  protected readonly offer = signal<JobOffer | null>(null);
  protected readonly applications = signal<JobApplication[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly applyError = signal<string | null>(null);
  protected readonly applySuccess = signal<string | null>(null);
  protected readonly deleting = signal(false);
  protected readonly applying = signal(false);

  protected readonly coverLetter = signal('');
  protected cvFile: File | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.errorMessage.set('Identifiant invalide');
      return;
    }
    this.load(id);
  }

  protected onCvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.cvFile = input.files?.[0] ?? null;
  }

  protected apply(): void {
    const offer = this.offer();
    if (!offer || this.applying()) {
      return;
    }
    if (!this.cvFile) {
      this.applyError.set('Selectionnez un CV au format PDF');
      return;
    }
    if (this.cvFile.type !== 'application/pdf') {
      this.applyError.set('Le CV doit etre un fichier PDF');
      return;
    }

    this.applyError.set(null);
    this.applySuccess.set(null);
    this.applying.set(true);

    this.recruitmentService.apply(offer.id, this.cvFile, this.coverLetter()).subscribe({
      next: () => {
        this.applySuccess.set('Candidature envoyee');
        this.applying.set(false);
        this.cvFile = null;
        this.coverLetter.set('');
        this.load(offer.id);
      },
      error: (error) => {
        this.applyError.set(extractErrorMessage(error, 'Candidature impossible'));
        this.applying.set(false);
      }
    });
  }

  protected updateStatus(application: JobApplication, status: ApplicationStatus): void {
    this.recruitmentService.updateApplicationStatus(application.id, status).subscribe({
      next: (updated) => {
        this.applications.update((list) =>
          list.map((item) => item.id === updated.id ? updated : item));
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Mise a jour du statut impossible'))
    });
  }

  protected remove(): void {
    const offer = this.offer();
    if (!offer) {
      return;
    }
    if (!confirm(`Supprimer l'offre « ${offer.title} » et toutes ses candidatures ?`)) {
      return;
    }
    this.deleting.set(true);
    this.recruitmentService.deleteOffer(offer.id).subscribe({
      next: () => this.router.navigate(['/recrutement']),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Suppression impossible'));
        this.deleting.set(false);
      }
    });
  }

  private load(id: number): void {
    this.recruitmentService.getOffer(id).subscribe({
      next: (offer) => {
        this.offer.set(offer);
        if (this.isAdmin()) {
          this.loadApplications(id);
        }
      },
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Offre introuvable'))
    });
  }

  private loadApplications(offerId: number): void {
    this.recruitmentService.listApplications(offerId).subscribe({
      next: (apps) => this.applications.set(apps),
      error: (error) => this.errorMessage.set(
        extractErrorMessage(error, 'Impossible de charger les candidatures'))
    });
  }
}
