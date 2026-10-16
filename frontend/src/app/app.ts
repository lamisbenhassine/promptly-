import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from './core/services/auth.service';
import { Footer } from './shared/components/footer/footer';
import { Navbar } from './shared/components/navbar/navbar';
import { Sidebar, SidebarLink } from './shared/components/sidebar/sidebar';

/** Routes sans barre laterale, barre superieure ni pied de page. */
const AUTH_SHELL_PATHS = new Set(['/connexion', '/inscription']);

/**
 * Composant racine : barre laterale (ouvrable / fermable), barre superieure
 * (marque + recherche + compte), zone de contenu et pied de page.
 * Sur connexion / inscription, seul le contenu auth est affiche.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, Sidebar, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /** Fermee par defaut ; le bouton ☰ ouvre / ferme la barre laterale. */
  protected readonly sidebarOpen = signal(false);
  protected readonly isAdmin = this.authService.isAdmin;

  /** Vrai sur les pages de connexion / inscription (sans chrome). */
  protected readonly isAuthShell = signal(this.isAuthUrl(this.router.url));

  protected readonly links = computed<SidebarLink[]>(() => [
    { label: 'Cours', path: '/cours', icon: '📘', enabled: true },
    { label: 'Quiz', path: '/quiz', icon: '📝', enabled: true },
    { label: 'Forum', path: '/forum', icon: '💬', enabled: true },
    { label: 'Reclamations', path: '/reclamations', icon: '📩', enabled: true },
    { label: 'Recrutement', path: '/recrutement', icon: '💼', enabled: true },
    { label: 'Assistant IA', path: '/ia', icon: '✨', enabled: true }
  ]);

  constructor() {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe((event) => {
      const auth = this.isAuthUrl(event.urlAfterRedirects);
      this.isAuthShell.set(auth);
      if (auth) {
        this.sidebarOpen.set(false);
      }
    });
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  private isAuthUrl(url: string): boolean {
    const path = url.split('?')[0];
    return AUTH_SHELL_PATHS.has(path);
  }
}
