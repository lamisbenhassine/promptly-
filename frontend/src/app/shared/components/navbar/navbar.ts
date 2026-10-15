import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { ROLE_LABELS } from '../../../core/models/user.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class Navbar {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.authService.currentUser;
  protected readonly isAuthenticated = this.authService.isAuthenticated;
  protected readonly isAdmin = this.authService.isAdmin;
  protected readonly roleLabels = ROLE_LABELS;

  /**
   * Liens de navigation. Les modules non encore developpes sont marques
   * comme desactives : ils seront actives au fur et a mesure des phases.
   */
  protected readonly links = [
    { label: 'Accueil', path: '/', enabled: true },
    { label: 'Cours', path: '/cours', enabled: true },
    { label: 'Quiz', path: '/quiz', enabled: false },
    { label: 'Forum', path: '/forum', enabled: false },
    { label: 'Reclamations', path: '/reclamations', enabled: false }
  ];

  protected logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
