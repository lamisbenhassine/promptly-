/**
 * Represente la reponse JSON de l'endpoint GET /api/health du backend.
 * Les noms de champs correspondent exactement au record Java HealthResponse.
 */
export interface Health {
  status: string;
  application: string;
  version: string;
  database: string;
  timestamp: string;
}
