import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ROLE_LABELS } from '../../../core/models/user.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [FormsModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly sidebarOpen = input(false);
  readonly menuToggle = output<void>();

  protected readonly user = this.authService.currentUser;
  protected readonly isAuthenticated = this.authService.isAuthenticated;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly searchQuery = signal('');

  protected toggleMenu(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.menuToggle.emit();
  }

  protected search(): void {
    const query = this.searchQuery().trim();
    if (!query) {
      return;
    }
    this.router.navigate(['/recherche'], { queryParams: { q: query } });
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigate(['/connexion']);
  }
}
