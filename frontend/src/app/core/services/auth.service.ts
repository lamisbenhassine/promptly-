import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AuthResult,
  LoginPayload,
  RegisterPayload,
  Role,
  User
} from '../models/user.model';

/**
 * Gestion de l'etat d'authentification du cote navigateur.
 *
 * <p><b>Choix de stockage.</b> Le jeton est conserve dans {@code localStorage}.
 * Cette solution est simple et permet de rester connecte apres un
 * rafraichissement de page, mais elle presente une limite reelle : un script
 * malveillant injecte dans la page (faille XSS) pourrait lire le jeton. Un
 * cookie {@code httpOnly} serait plus sur, au prix d'une protection CSRF
 * supplementaire. Ce compromis est assume et documente dans le rapport.</p>
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private static readonly TOKEN_KEY = 'promptly.token';
  private static readonly USER_KEY = 'promptly.user';

  private readonly http = inject(HttpClient);

  /**
   * Utilisateur connecte, expose sous forme de signal : les composants qui le
   * lisent sont automatiquement reactualises lors d'une connexion ou d'une
   * deconnexion, sans abonnement manuel.
   */
  private readonly userSignal = signal<User | null>(this.readStoredUser());

  readonly currentUser = this.userSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.userSignal() !== null);
  readonly isAdmin = computed(() => this.userSignal()?.role === 'ADMIN');
  readonly canManageCourses = computed(() => {
    const role = this.userSignal()?.role;
    return role === 'ADMIN' || role === 'ENSEIGNANT';
  });

  // ----------------------------------------------------------------
  //  Appels a l'API
  // ----------------------------------------------------------------

  register(payload: RegisterPayload): Observable<User> {
    return this.http.post<User>(`${environment.apiBaseUrl}/auth/register`, payload);
  }

  login(payload: LoginPayload): Observable<AuthResult> {
    return this.http.post<AuthResult>(`${environment.apiBaseUrl}/auth/login`, payload)
      .pipe(tap((result) => this.storeSession(result)));
  }

  /**
   * Recharge le profil depuis le serveur.
   *
   * Utile car les donnees conservees localement peuvent etre perimees : un
   * administrateur a pu modifier le role ou desactiver le compte entre-temps.
   */
  loadCurrentUser(): Observable<User> {
    return this.http.get<User>(`${environment.apiBaseUrl}/auth/me`)
      .pipe(tap((user) => this.updateStoredUser(user)));
  }

  // ----------------------------------------------------------------
  //  Etat local
  // ----------------------------------------------------------------

  logout(): void {
    localStorage.removeItem(AuthService.TOKEN_KEY);
    localStorage.removeItem(AuthService.USER_KEY);
    this.userSignal.set(null);
  }

  token(): string | null {
    return localStorage.getItem(AuthService.TOKEN_KEY);
  }

  hasRole(role: Role): boolean {
    return this.userSignal()?.role === role;
  }

  /** Met a jour le profil memorise apres une modification reussie. */
  updateStoredUser(user: User): void {
    localStorage.setItem(AuthService.USER_KEY, JSON.stringify(user));
    this.userSignal.set(user);
  }

  private storeSession(result: AuthResult): void {
    localStorage.setItem(AuthService.TOKEN_KEY, result.token);
    this.updateStoredUser(result.user);
  }

  /**
   * Relit la session depuis localStorage au demarrage de l'application.
   * Le contenu est traite avec prudence : il peut avoir ete altere.
   */
  private readStoredUser(): User | null {
    const token = localStorage.getItem(AuthService.TOKEN_KEY);
    const raw = localStorage.getItem(AuthService.USER_KEY);

    if (!token || !raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as User;
    } catch {
      // Donnees corrompues : on repart d'une session vierge.
      this.logout();
      return null;
    }
  }
}
