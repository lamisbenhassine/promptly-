import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { extractErrorMessage } from '../../../core/models/api-error.model';
import { ROLE_LABELS, Role, User } from '../../../core/models/user.model';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-users',
  imports: [DatePipe, FormsModule],
  templateUrl: './users.html',
  styleUrl: './users.css'
})
export class Users implements OnInit {

  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);

  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly infoMessage = signal<string | null>(null);
  protected readonly pendingId = signal<number | null>(null);

  protected readonly roles: Role[] = ['ETUDIANT', 'ENSEIGNANT', 'ADMIN'];
  protected readonly roleLabels = ROLE_LABELS;

  ngOnInit(): void {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.userService.list().subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'Impossible de charger les utilisateurs'));
        this.loading.set(false);
      }
    });
  }

  protected changeRole(user: User, role: Role): void {
    if (user.role === role) {
      return;
    }

    this.pendingId.set(user.id);
    this.infoMessage.set(null);
    this.errorMessage.set(null);

    this.userService.updateRole(user.id, role).subscribe({
      next: (updated) => {
        this.replace(updated);
        this.infoMessage.set(`Role de ${updated.email} mis a jour : ${ROLE_LABELS[updated.role]}.`);
        this.pendingId.set(null);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La modification du role a echoue'));
        this.pendingId.set(null);
      }
    });
  }

  protected toggleStatus(user: User): void {
    this.pendingId.set(user.id);
    this.infoMessage.set(null);
    this.errorMessage.set(null);

    this.userService.updateStatus(user.id, !user.enabled).subscribe({
      next: (updated) => {
        this.replace(updated);
        const action = updated.enabled ? 'reactive' : 'desactive';
        this.infoMessage.set(`Compte ${updated.email} ${action}.`);
        this.pendingId.set(null);
      },
      error: (error) => {
        this.errorMessage.set(extractErrorMessage(error, 'La modification du statut a echoue'));
        this.pendingId.set(null);
      }
    });
  }

  /** Empêche un administrateur de se desactiver lui-meme depuis cette page. */
  protected isCurrentUser(user: User): boolean {
    return this.authService.currentUser()?.id === user.id;
  }

  private replace(updated: User): void {
    this.users.update((list) => list.map((item) => item.id === updated.id ? updated : item));
  }
}
