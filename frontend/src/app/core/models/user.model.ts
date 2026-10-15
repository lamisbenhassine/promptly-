/** Roles definis cote backend (enumeration Java Role). */
export type Role = 'ETUDIANT' | 'ENSEIGNANT' | 'ADMIN';

/** Libelles affichables des roles. */
export const ROLE_LABELS: Record<Role, string> = {
  ETUDIANT: 'Etudiant',
  ENSEIGNANT: 'Enseignant',
  ADMIN: 'Administrateur'
};

/** Correspond au record Java UserResponse. */
export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

/** Reponse de POST /api/auth/login. */
export interface AuthResult {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface UpdateProfilePayload {
  firstName: string;
  lastName: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

/** Enveloppe de pagination renvoyee par Spring Data. */
export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
