import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { ROLE_LABELS } from '../../../core/models/user.model';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile {

  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);

  protected readonly user = this.authService.currentUser;
  protected readonly roleLabels = ROLE_LABELS;

  protected readonly profileSubmitting = signal(false);
  protected readonly passwordSubmitting = signal(false);
  protected readonly profileMessage = signal<string | null>(null);
  protected readonly profileError = signal<string | null>(null);
  protected readonly passwordMessage = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);

  protected readonly profileForm = this.formBuilder.nonNullable.group({
    firstName: [this.user()?.firstName ?? '', [
      Validators.required, Validators.minLength(2), Validators.maxLength(50)
    ]],
    lastName: [this.user()?.lastName ?? '', [
      Validators.required, Validators.minLength(2), Validators.maxLength(50)
    ]]
  });

  protected readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-zA-Z])(?=.*\d).+$/)
    ]]
  });

  protected saveProfile(): void {
    this.profileMessage.set(null);
    this.profileError.set(null);

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.profileSubmitting.set(true);

    this.userService.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: (updated) => {
        this.authService.updateStoredUser(updated);
        this.profileMessage.set('Profil mis a jour.');
        this.profileSubmitting.set(false);
      },
      error: (error) => {
        this.profileError.set(extractErrorMessage(error, 'La mise a jour a echoue'));
        this.profileSubmitting.set(false);
      }
    });
  }

  protected changePassword(): void {
    this.passwordMessage.set(null);
    this.passwordError.set(null);

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.passwordSubmitting.set(true);

    this.userService.changePassword(this.passwordForm.getRawValue()).subscribe({
      next: () => {
        this.passwordMessage.set('Mot de passe change.');
        this.passwordForm.reset();
        this.passwordSubmitting.set(false);
      },
      error: (error) => {
        this.passwordError.set(extractErrorMessage(error, 'Le changement de mot de passe a echoue'));
        this.passwordSubmitting.set(false);
      }
    });
  }

  protected showProfileError(field: string): boolean {
    const control = this.profileForm.get(field);
    return !!control && control.invalid && control.touched;
  }

  protected showPasswordError(field: string): boolean {
    const control = this.passwordForm.get(field);
    return !!control && control.invalid && control.touched;
  }
}
