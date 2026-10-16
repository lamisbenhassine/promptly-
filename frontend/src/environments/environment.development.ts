/**
 * Configuration utilisee par le serveur de developpement (`ng serve`).
 * Le backend Spring Boot ecoute sur le port 8080.
 *
 * googleClientId : Client ID OAuth "Application Web" (Google Cloud Console).
 * Collez la meme valeur que PROMPTLY_GOOGLE_CLIENT_ID cote backend.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  /** Origine du backend pour construire les URL d'images et de PDF. */
  apiOrigin: 'http://localhost:8080',
  /** Client ID OAuth Web (Google Cloud Console). Public cote navigateur. */
  googleClientId: '89784180320-33tpa3ee3jq4cokea8cd930ha43icf7t.apps.googleusercontent.com'
};
