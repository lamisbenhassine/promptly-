import { Routes } from '@angular/router';

import { authGuard, roleGuard } from './core/guards/auth.guard';

/**
 * Table de routage de l'application.
 *
 * Les pages protegees passent par {@code authGuard} (connexion requise) et,
 * pour l'administration, par {@code roleGuard(['ADMIN'])}. Ces gardes sont
 * un confort d'interface : la protection reelle est appliquee cote serveur.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    title: 'Promptly - Accueil'
  },
  {
    path: 'inscription',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
    title: 'Promptly - Inscription'
  },
  {
    path: 'connexion',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    title: 'Promptly - Connexion'
  },
  {
    path: 'profil',
    loadComponent: () => import('./features/auth/profile/profile').then((m) => m.Profile),
    canActivate: [authGuard],
    title: 'Promptly - Mon profil'
  },
  {
    path: 'admin/utilisateurs',
    loadComponent: () => import('./features/admin/users/users').then((m) => m.Users),
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    title: 'Promptly - Utilisateurs'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
