import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/user.model';
import { AuthService } from '../services/auth.service';

/**
 * Interdit l'acces a une route aux visiteurs non connectes.
 *
 * Il s'agit d'un confort d'utilisation, non d'une mesure de securite : les
 * donnees restent protegees par le backend. Un utilisateur qui contournerait
 * cette garde n'obtiendrait que des reponses 401.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // L'URL demandee est memorisee afin d'y revenir apres la connexion.
  return router.createUrlTree(['/connexion'], {
    queryParams: { redirect: state.url }
  });
};

/**
 * Reserve les pages de connexion / inscription aux visiteurs non connectes.
 * Un utilisateur deja authentifie est renvoye vers les cours.
 */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/cours']);
};

/**
 * Page d'entree : connexion si anonyme, cours si deja connecte.
 */
export const landingRedirectGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated()
    ? router.createUrlTree(['/cours'])
    : router.createUrlTree(['/connexion']);
};

/**
 * Restreint une route a certains roles.
 *
 * S'utilise dans la table de routage : {@code canActivate: [authGuard, roleGuard(['ADMIN'])]}
 */
export function roleGuard(allowedRoles: Role[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const user = authService.currentUser();

    if (user && allowedRoles.includes(user.role)) {
      return true;
    }

    return router.createUrlTree(['/cours']);
  };
}
