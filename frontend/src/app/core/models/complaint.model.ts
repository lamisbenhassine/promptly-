import { Role } from './user.model';

export type ComplaintCategory = 'TECHNIQUE' | 'PEDAGOGIQUE' | 'COMPTE' | 'AUTRE';
export type ComplaintStatus = 'OUVERTE' | 'EN_COURS' | 'RESOLUE' | 'REJETEE';

export const COMPLAINT_CATEGORIES: ComplaintCategory[] = [
  'TECHNIQUE', 'PEDAGOGIQUE', 'COMPTE', 'AUTRE'
];

export const COMPLAINT_STATUSES: ComplaintStatus[] = [
  'OUVERTE', 'EN_COURS', 'RESOLUE', 'REJETEE'
];

export const COMPLAINT_CATEGORY_LABELS: Record<ComplaintCategory, string> = {
  TECHNIQUE: 'Technique',
  PEDAGOGIQUE: 'Pedagogique',
  COMPTE: 'Compte',
  AUTRE: 'Autre'
};

export const COMPLAINT_STATUS_LABELS: Record<ComplaintStatus, string> = {
  OUVERTE: 'Ouverte',
  EN_COURS: 'En cours',
  RESOLUE: 'Resolue',
  REJETEE: 'Rejetee'
};

export interface ComplaintAuthor {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface Complaint {
  id: number;
  subject: string;
  description: string;
  category: ComplaintCategory;
  status: ComplaintStatus;
  author: ComplaintAuthor;
  adminResponse: string | null;
  handledBy: ComplaintAuthor | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
}

export interface ComplaintPayload {
  subject: string;
  description: string;
  category: ComplaintCategory;
}

export interface ComplaintStatusPayload {
  status: ComplaintStatus;
  adminResponse?: string | null;
}
