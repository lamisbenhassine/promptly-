import { DatePipe, SlicePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  CONTRACT_TYPE_LABELS,
  JobOffer
} from '../../../core/models/recruitment.model';
import { AuthService } from '../../../core/services/auth.service';
import { RecruitmentService } from '../../../core/services/recruitment.service';

@Component({
  selector: 'app-offer-list',
  imports: [DatePipe, SlicePipe, RouterLink],
  templateUrl: './offer-list.html',
  styleUrl: './offer-list.css'
})
export class OfferList implements OnInit {

  private readonly recruitmentService = inject(RecruitmentService);
  private readonly authService = inject(AuthService);

  protected readonly contractLabels = CONTRACT_TYPE_LABELS;
  protected readonly isAdmin = this.authService.isAdmin;

  protected readonly offers = signal<JobOffer[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.recruitmentService.listOffers().subscribe({
      next: (offers) => {
        this.offers.set(offers);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de charger les offres'));
        this.loading.set(false);
      }
    });
  }
}
