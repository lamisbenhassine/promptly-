import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html'
})
export class Login {

  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly infoMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  constructor() {
    const params = this.route.snapshot.queryParamMap;

    if (params.get('inscrit') === 'true') {
      this.infoMessage.set('Compte cree. Vous pouvez maintenant vous connecter.');
    } else if (params.get('expire') === 'true') {
      this.infoMessage.set('Votre session a expire. Reconnectez-vous.');
    }
  }

  protected submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        // Retour a la page demandee avant la redirection, ou a l'accueil.
        const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/';
        this.router.navigateByUrl(redirect);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Identifiants incorrects'));
        this.submitting.set(false);
      }
    });
  }

  protected showError(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }
}
