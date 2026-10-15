# Promptly

Plateforme e-learning academique dediee a l'apprentissage de la programmation,
du developpement logiciel et de l'algorithmique (Java, Spring Boot, Angular,
TypeScript, structures de donnees).

Le projet est developpe de maniere incrementale, module par module, et
documente l'utilisation de l'intelligence artificielle a deux niveaux :

- **l'IA comme assistant de developpement** (Cursor) : voir `docs/rapport/03-utilisation-ia-developpement.md` ;
- **l'IA integree a l'application** (fonctionnalite a definir) : voir `docs/rapport/04-integration-ia-application.md`.

## Etat d'avancement

| Phase | Module | Etat |
|-------|--------|------|
| 0 | Template et fondation technique | **Termine** |
| 1 | Authentification et gestion des utilisateurs | **Termine** |
| 2 | Gestion des cours | Non commence |
| 3 | Gestion des chapitres | Non commence |
| 4 | Evaluations et quiz | Non commence |
| 5 | Forum et communaute | Non commence |
| 6 | Gestion des reclamations | Non commence |
| 7 | Integration de l'IA | Non commence |

## Architecture

```
ia/
├── backend/     API REST Spring Boot (logique metier, acces aux donnees)
├── frontend/    Application Angular (interface utilisateur)
└── docs/        Rapport academique et captures d'ecran
```

| Couche | Responsabilite |
|--------|----------------|
| Frontend Angular | Affichage, navigation, formulaires, appels HTTP |
| API REST | Contrat d'echange en JSON entre Angular et Spring Boot |
| Backend Spring Boot | Regles metier, validation, securite, transactions |
| MySQL | Persistance et integrite des donnees |

## Technologies

### Backend

| Technologie | Version | Role |
|-------------|---------|------|
| Java (JDK) | 17 | Langage et plateforme d'execution |
| Spring Boot | 3.5.16 | Socle applicatif et auto-configuration |
| Spring Web | heritee | Controleurs REST, serveur Tomcat embarque |
| Spring Data JPA | heritee | ORM Hibernate et repositories |
| Bean Validation | heritee | Validation des donnees entrantes |
| MySQL Connector/J | heritee | Pilote JDBC |
| Maven | 3.9.11 (wrapper inclus) | Gestion des dependances et build |

Les versions des dependances ne sont pas fixees individuellement : elles sont
heritees de `spring-boot-starter-parent`, qui garantit leur compatibilite.

Spring Boot **3.5.x** a ete retenu plutot que 4.x car le support de Spring AI
(utilise ulterieurement pour l'integration d'un modele de langage) y est
stabilise, et la documentation disponible porte majoritairement sur cette
ligne de versions.

### Frontend

| Technologie | Version | Role |
|-------------|---------|------|
| Angular | 20 | Framework d'interface |
| TypeScript | 5.9 | Langage |
| Angular Router | 20 | Navigation entre les pages |
| RxJS | 7.8 | Flux asynchrones (reponses HTTP) |
| CSS | - | Mise en forme responsive, variables CSS |

Angular 20 a ete choisi car Angular 18 ne supporte pas Node.js 24 installe sur
la machine de developpement (`ng version` signalait explicitement
`Node: 24.13.1 (Unsupported)`).

## Prerequis

| Logiciel | Version minimale | Verification |
|----------|------------------|--------------|
| JDK | 17 | `java -version` |
| Node.js | 20.19+ | `node -v` |
| Angular CLI | 20 | `ng version` |
| MySQL Server | 8.0 | `mysql --version` |
| Git | 2.x | `git --version` |

Maven n'a pas besoin d'etre installe : le projet inclut le wrapper `mvnw.cmd`.

## Installation

### 1. Cloner le depot

```powershell
git clone https://github.com/lamisbenhassine/promptly-.git
cd promptly-
```

### 2. Creer la base de donnees

Depuis MySQL Workbench ou en ligne de commande :

```sql
CREATE DATABASE IF NOT EXISTS promptly_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Aucune table n'est a creer manuellement : Hibernate les generera a partir des
entites Java (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Configurer les identifiants de la base

Les identifiants MySQL ne sont **jamais ecrits dans le code**. Ils sont lus
depuis des variables d'environnement Windows, a definir une seule fois :

```powershell
[Environment]::SetEnvironmentVariable('PROMPTLY_DB_USER','root','User')
[Environment]::SetEnvironmentVariable('PROMPTLY_DB_PASSWORD','votre_mot_de_passe','User')

# Cle de signature JWT : au moins 32 caracteres, generee aleatoirement.
[Environment]::SetEnvironmentVariable('PROMPTLY_JWT_SECRET','<cle-d-au-moins-32-caracteres>','User')

# Compte administrateur cree au premier demarrage s'il n'en existe aucun.
[Environment]::SetEnvironmentVariable('PROMPTLY_ADMIN_EMAIL','admin@promptly.local','User')
[Environment]::SetEnvironmentVariable('PROMPTLY_ADMIN_PASSWORD','<mot-de-passe-admin>','User')
```

Fermer puis reouvrir le terminal pour que les variables soient prises en compte.
Verification :

```powershell
$env:PROMPTLY_DB_USER
```

### 4. Installer les dependances du frontend

```powershell
cd frontend
npm install
```

## Lancement

Deux terminaux sont necessaires.

**Terminal 1 - backend** (http://localhost:8080) :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Terminal 2 - frontend** (http://localhost:4200) :

```powershell
cd frontend
npm start
```

Ouvrir ensuite http://localhost:4200 dans un navigateur.

## Tests

**Backend** (JUnit 5 + MockMvc) :

```powershell
cd backend
.\mvnw.cmd test
```

**Frontend** (Karma + Jasmine) :

```powershell
cd frontend
npm test
```

**Compilation de production du frontend** :

```powershell
cd frontend
npm run build
```

## API

| Methode | Endpoint | Acces | Description |
|---------|----------|-------|-------------|
| GET | `/api/health` | Public | Verifie que l'API repond et que MySQL est joignable |
| POST | `/api/auth/register` | Public | Cree un compte ETUDIANT |
| POST | `/api/auth/login` | Public | Renvoie un jeton JWT |
| GET | `/api/auth/me` | Authentifie | Profil de l'utilisateur connecte |
| PUT | `/api/users/me` | Authentifie | Modification du profil |
| PUT | `/api/users/me/password` | Authentifie | Changement de mot de passe |
| GET | `/api/users` | ADMIN | Liste paginee des utilisateurs |
| PUT | `/api/users/{id}/role` | ADMIN | Modification du role |
| PATCH | `/api/users/{id}/status` | ADMIN | Activation / desactivation |

Exemple de reponse :

```json
{
  "status": "UP",
  "application": "Promptly",
  "version": "0.0.1-SNAPSHOT",
  "database": "UP",
  "timestamp": "2026-10-15T18:33:54.617089400Z"
}
```

Le champ `database` vaut `UP` si la connexion MySQL est valide, sinon
`DOWN: <message d'erreur>`. L'API continue de repondre meme si la base est
arretee, ce qui facilite le diagnostic.

## Documentation

| Fichier | Contenu |
|---------|---------|
| `docs/rapport/01-presentation-projet.md` | Contexte, objectifs, perimetre |
| `docs/rapport/02-architecture-et-conception.md` | Architecture, choix techniques |
| `docs/rapport/03-utilisation-ia-developpement.md` | L'IA comme assistant de developpement |
| `docs/rapport/04-integration-ia-application.md` | L'IA integree au produit |
| `docs/rapport/05-tests-et-validation.md` | Tests realises et resultats |
| `docs/rapport/06-captures-ecran.md` | Liste des figures du rapport |
| `docs/rapport/journal-prompts.md` | Journal chronologique des prompts |
| `docs/screenshots/` | Captures d'ecran, classees par module |
