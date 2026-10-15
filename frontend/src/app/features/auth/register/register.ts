import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html'
})
export class Register {

  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  /**
   * Formulaire reactif. Les regles de validation reproduisent celles du
   * backend (Bean Validation) afin que l'utilisateur soit averti
   * immediatement, sans attendre un aller-retour reseau. Le backend reste
   * toutefois seul garant : ces controles peuvent etre contournes.
   */
  protected readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      // Au moins une lettre et un chiffre, identique au @Pattern du backend.
      Validators.pattern(/^(?=.*[a-zA-Z])(?=.*\d).+$/)
    ]]
  });

  protected submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      // Marque tous les champs comme "touches" pour afficher les messages.
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        // Compte cree : on redirige vers la connexion avec un indicateur.
        this.router.navigate(['/connexion'], { queryParams: { inscrit: 'true' } });
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, "L'inscription a echoue"));
        this.submitting.set(false);
      }
    });
  }

  /** Indique si un champ doit afficher son message d'erreur. */
  protected showError(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }
}
