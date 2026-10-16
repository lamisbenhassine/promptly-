import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import {
  CONTRACT_TYPES,
  CONTRACT_TYPE_LABELS,
  ContractType,
  JobOfferPayload
} from '../../../core/models/recruitment.model';
import { RecruitmentService } from '../../../core/services/recruitment.service';

@Component({
  selector: 'app-offer-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './offer-form.html'
})
export class OfferForm implements OnInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly recruitmentService = inject(RecruitmentService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly contractTypes = CONTRACT_TYPES;
  protected readonly contractLabels = CONTRACT_TYPE_LABELS;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly editId = signal<number | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(180)]],
    description: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(8000)]],
    skills: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2000)]],
    experience: [''],
    location: [''],
    contractType: ['STAGE' as ContractType, Validators.required],
    published: [true],
    deadline: ['']
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.editId.set(id);
      this.recruitmentService.getOffer(id).subscribe({
        next: (offer) => {
          this.form.patchValue({
            title: offer.title,
            description: offer.description,
            skills: offer.skills ?? '',
            experience: offer.experience ?? '',
            location: offer.location ?? '',
            contractType: offer.contractType,
            published: offer.published,
            deadline: offer.deadline ?? ''
          });
        },
        error: (error) => this.errorMessage.set(
          extractErrorMessage(error, 'Offre introuvable'))
      });
    }
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload: JobOfferPayload = {
      title: raw.title,
      description: raw.description,
      skills: raw.skills.trim(),
      experience: raw.experience.trim() || null,
      location: raw.location.trim() || null,
      contractType: raw.contractType,
      published: raw.published,
      deadline: raw.deadline || null
    };

    this.submitting.set(true);
    const id = this.editId();
    const request$ = id
      ? this.recruitmentService.updateOffer(id, payload)
      : this.recruitmentService.createOffer(payload);

    request$.subscribe({
      next: (offer) => this.router.navigate(['/recrutement', offer.id]),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Enregistrement impossible'));
        this.submitting.set(false);
      }
    });
  }

  protected showError(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }
}
