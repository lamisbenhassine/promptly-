import { AfterViewInit, Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleAuthService } from '../../../core/services/google-auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register implements AfterViewInit {

  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly googleAuth = inject(GoogleAuthService);
  private readonly router = inject(Router);

  private readonly googleButton = viewChild<ElementRef<HTMLElement>>('googleButton');

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly googleEnabled = this.googleAuth.isConfigured;

  protected readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-zA-Z])(?=.*\d).+$/)
    ]]
  });

  ngAfterViewInit(): void {
    if (!this.googleEnabled) {
      return;
    }

    const host = this.googleButton()?.nativeElement;
    if (!host) {
      return;
    }

    this.googleAuth.renderButton(host, (idToken) => this.onGoogleCredential(idToken), 'signup_with')
      .catch(() => {
        this.errorMessage.set('Impossible d\'afficher l\'inscription Google.');
      });
  }

  protected submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.router.navigate(['/connexion'], { queryParams: { inscrit: 'true' } });
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, "L'inscription a echoue"));
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
      next: () => this.router.navigateByUrl('/cours'),
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Inscription Google impossible'));
        this.submitting.set(false);
      }
    });
  }
}
