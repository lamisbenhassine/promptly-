import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { ComplaintPayload } from '../../../core/models/complaint.model';
import { ComplaintService } from '../../../core/services/complaint.service';

@Component({
  selector: 'app-complaint-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './complaint-form.html'
})
export class ComplaintForm {

  private readonly formBuilder = inject(FormBuilder);
  private readonly complaintService = inject(ComplaintService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    subject: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(180)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(5000)]]
  });

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: ComplaintPayload = this.form.getRawValue();
    this.submitting.set(true);
    this.complaintService.create(payload).subscribe({
      next: (complaint) => this.router.navigate(['/reclamations', complaint.id]),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Le depot a echoue'));
        this.submitting.set(false);
      }
    });
  }

  protected showError(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }
}
