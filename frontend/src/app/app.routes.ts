import { Routes } from '@angular/router';

/**
 * Table de routage de l'application.
 *
 * Le chargement differe (`loadComponent`) permet a Angular de ne telecharger
 * le code d'une page que lorsque l'utilisateur la visite. Les routes des
 * modules metier seront ajoutees ici au fur et a mesure des phases.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    title: 'Promptly - Accueil'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
