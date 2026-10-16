import { Role } from './user.model';

export type ContractType = 'CDI' | 'CDD' | 'STAGE' | 'ALTERNANCE' | 'FREELANCE' | 'AUTRE';
export type ApplicationStatus = 'ENVOYEE' | 'VUE' | 'ACCEPTEE' | 'REFUSEE';

export const CONTRACT_TYPES: ContractType[] = [
  'STAGE', 'ALTERNANCE', 'CDI', 'CDD', 'FREELANCE', 'AUTRE'
];

export const CONTRACT_TYPE_LABELS: Record<ContractType, string> = {
  CDI: 'CDI',
  CDD: 'CDD',
  STAGE: 'Stage',
  ALTERNANCE: 'Alternance',
  FREELANCE: 'Freelance',
  AUTRE: 'Autre'
};

export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'ENVOYEE', 'VUE', 'ACCEPTEE', 'REFUSEE'
];

export const APPLICATION_STATUS_LABELS: Record<ApplicationStatus, string> = {
  ENVOYEE: 'Envoyee',
  VUE: 'Vue',
  ACCEPTEE: 'Acceptee',
  REFUSEE: 'Refusee'
};

export interface ApplicantSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface JobOffer {
  id: number;
  title: string;
  description: string;
  skills: string | null;
  experience: string | null;
  location: string | null;
  contractType: ContractType;
  published: boolean;
  deadline: string | null;
  createdBy: ApplicantSummary;
  applicationCount: number;
  alreadyApplied: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface JobOfferPayload {
  title: string;
  description: string;
  skills: string;
  experience: string | null;
  location: string | null;
  contractType: ContractType;
  published: boolean;
  deadline: string | null;
}

export interface JobApplication {
  id: number;
  offerId: number;
  offerTitle: string;
  applicant: ApplicantSummary;
  cvUrl: string | null;
  message: string | null;
  status: ApplicationStatus;
  matchScore: number | null;
  matchedSkills: string | null;
  missingSkills: string | null;
  experienceMatched: boolean | null;
  matchProvider: string | null;
  createdAt: string;
}
