import { AfterViewInit, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleAuthService } from '../../../core/services/google-auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements AfterViewInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly googleAuth = inject(GoogleAuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly googleButton = viewChild<ElementRef<HTMLElement>>('googleButton');

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly infoMessage = signal<string | null>(null);
  protected readonly googleEnabled = this.googleAuth.isConfigured;

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

  ngAfterViewInit(): void {
    if (!this.googleEnabled) {
      return;
    }

    const host = this.googleButton()?.nativeElement;
    if (!host) {
      return;
    }

    this.googleAuth.renderButton(host, (idToken) => this.onGoogleCredential(idToken), 'signin_with')
      .catch(() => {
        this.errorMessage.set('Impossible d\'afficher la connexion Google.');
      });
  }

  protected submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => this.afterAuthSuccess(),
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

  private onGoogleCredential(idToken: string): void {
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.authService.loginWithGoogle({ idToken }).subscribe({
      next: () => this.afterAuthSuccess(),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Connexion Google impossible'));
        this.submitting.set(false);
      }
    });
  }

  private afterAuthSuccess(): void {
    const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/cours';
    this.router.navigateByUrl(redirect);
  }
}
