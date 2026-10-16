# Journal chronologique des prompts

## Objet de ce document

Ce journal enregistre, dans l'ordre chronologique, les échanges avec l'outil d'IA
utilisé pendant le développement. Il constitue la **trace brute** des
interactions ; l'analyse structurée figure au chapitre 3 du rapport.

**Règle de tenue :** seuls des prompts réellement formulés et des résultats
réellement obtenus sont consignés. Aucune reconstitution a posteriori.

| Métadonnée | Valeur |
|------------|--------|
| Outil | Cursor, mode Agent |
| Modèle | Claude (Anthropic) — identifiant de version non exposé par l'interface |
| Environnement | Windows 10, PowerShell |
| Dernière mise à jour | 15 octobre 2026 |

---

## Phase 0 — Template et fondation

### P-001 — Cahier des charges et lancement de la phase 0

**Date :** 15 octobre 2026
**Objectif :** définir le projet, imposer la méthode de travail, lancer la phase 0.

**Prompt (extraits ; environ 300 lignes au total) :**

> « I want to develop an academic e-learning web application named **Promptly**,
> dedicated to teaching programming, software development, and algorithms. [...]
> The project uses **Spring Boot for the backend and Angular for the frontend**.
>
> **IMPORTANT: Do not generate the entire application in one prompt. We will
> develop it progressively, management by management, and I must confirm each
> step before you continue.**
>
> [...] Begin with **Phase 0: Project Template and Foundation**. First inspect
> the workspace and report what already exists. Then propose the folder
> structure, the technologies and compatible versions, the initialization
> commands, and the planned files. Do not implement the complete project. Do not
> start authentication or any other management module yet.
>
> Wait for my confirmation before making the initial structural changes. »

Le prompt comportait en outre : la liste des sept modules fonctionnels, la pile
technologique imposée, l'arborescence documentaire attendue
(`docs/rapport/`, `docs/screenshots/`), les règles de documentation de l'IA, et
les règles de qualité et de sécurité, dont :

> « Never hardcode passwords, tokens, API keys, or other secrets. »
> « Never claim that code compiles, tests pass, or a feature works without verification. »
> « Do not invent prompts, generated results, tests, screenshots, or AI contributions. »

**Contexte fourni :** répertoire de travail vide, Windows 10, PowerShell, accès
au terminal.

**Résultat obtenu :** l'agent a exécuté des commandes de diagnostic puis produit
un rapport d'état et une proposition d'architecture.

Versions détectées :

```
Java (JDK)    : 17.0.2 LTS
Maven         : 3.9.11
Node.js       : 24.13.1
npm           : 11.8.0
Angular CLI   : 18.2.21     → Node: 24.13.1 (Unsupported)
MySQL Server  : 8.0.46, service actif, port 3306 à l'écoute
Git           : 2.47.0, aucun dépôt initialisé
Ports 8080 et 4200 : libres
```

**Anomalie signalée par l'IA :** incompatibilité entre Angular CLI 18 et
Node.js 24 (anomalie A1 du chapitre 5). Trois options proposées.

**Problèmes constatés dans le résultat :**
1. Recommandation de Spring Boot 3.4.x **sans vérification** des versions
   disponibles (voir P-003).
2. Détection de cinq services MySQL, dont quatre résidus d'installations
   antérieures, nécessitant une interprétation humaine.

**Aucun fichier créé à ce stade** : la consigne d'attente de validation a été
respectée.

---

### P-002 — Arbitrage des décisions structurantes

**Date :** 15 octobre 2026
**Objectif :** trancher les trois points bloquants identifiés par l'IA.

**Questions posées par l'IA et réponses fournies :**

| Question | Réponse |
|----------|---------|
| Incompatibilité Angular 18 / Node 24 : comment la résoudre ? | Mettre à niveau Angular CLI vers la version 20 |
| Configuration de MySQL en phase 0 ? | MySQL Workbench installé ; mot de passe communiqué |
| Initialiser un dépôt Git ? | Dépôt GitHub existant : `https://github.com/lamisbenhassine/promptly-.git` |

**Résultat :** l'agent a signalé, sans y être invité, que le mot de passe ne
devait pas être inscrit dans un fichier versionné, le dépôt étant hébergé sur
GitHub, et qu'un secret présent dans un *commit* demeure accessible dans
l'historique même après suppression. Il a mis en place une lecture par variables
d'environnement.

**Modification appliquée au projet :**

```properties
spring.datasource.username=${PROMPTLY_DB_USER:root}
spring.datasource.password=${PROMPTLY_DB_PASSWORD:}
```

```powershell
[Environment]::SetEnvironmentVariable('PROMPTLY_DB_USER','root','User')
[Environment]::SetEnvironmentVariable('PROMPTLY_DB_PASSWORD','<mot_de_passe>','User')
```

**Tests effectués :** mise à niveau d'Angular CLI vérifiée.

```
Avant : Angular CLI: 18.2.21 — Node: 24.13.1 (Unsupported)
Après : Angular CLI: 20.3.40 — Node: 24.13.1
```

L'avertissement a disparu.

---

### P-003 — Correction de la version de Spring Boot

**Date :** 15 octobre 2026
**Objectif :** vérifier la disponibilité réelle de la version de Spring Boot
recommandée.

**Résultat initial à corriger :** l'agent avait recommandé Spring Boot 3.4.x en
invoquant la compatibilité de Spring AI 1.0.

**Vérification effectuée** (interrogation de l'API de Spring Initializr) :

```
DEFAULT BOOT: 4.1.1.RELEASE
--- VERSIONS DISPONIBLES ---
4.2.0.BUILD-SNAPSHOT
4.2.0.M2
4.1.2.BUILD-SNAPSHOT
4.1.1.RELEASE
4.0.9.BUILD-SNAPSHOT
4.0.8.RELEASE
```

Aucune version 3.x n'était proposée. La recommandation initiale reposait donc sur
une hypothèse issue des données d'entraînement du modèle, et non sur l'état
effectif de l'écosystème.

**Vérification complémentaire** (dépôt Maven Central) :

```powershell
$x = [xml](Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml" -UseBasicParsing).Content
$x.metadata.versioning.versions.version | Where-Object { $_ -like "3.5.*" }
```

Sortie : `3.5.0` à `3.5.16`.

**Correction appliquée :** Spring Boot **3.5.16**, avec justification révisée
(Spring AI 1.1 stabilisé sur la ligne 3.5 ; Spring Boot 4.x écarté car le support
de Spring AI y est encore en version *milestone*).

**Conséquence pratique :** Spring Initializr ne pouvant plus générer de projet
3.x, le fichier `pom.xml` a été rédigé manuellement et le *wrapper* Maven ajouté
par `mvn -N wrapper:wrapper`.

**Enseignement :** toute version de dépendance proposée par l'IA doit être
vérifiée auprès du dépôt officiel avant d'être inscrite dans un fichier de
configuration.

---

### P-004 — Génération du backend

**Date :** 15 octobre 2026
**Objectif :** produire un backend Spring Boot fonctionnel avec un endpoint de
vérification.

**Résultat obtenu :** sept fichiers créés.

| Fichier | Contenu |
|---------|---------|
| `backend/pom.xml` | Parent 3.5.16, Java 17, six dépendances |
| `PromptlyApplication.java` | Classe de démarrage |
| `config/CorsConfig.java` | Autorisation de l'origine Angular, lue depuis une propriété |
| `health/HealthController.java` | `GET /api/health` avec vérification JDBC |
| `health/HealthResponse.java` | `record` de réponse |
| `application.properties` | MySQL, JPA, CORS, journalisation |
| `HealthControllerTest.java` | Deux tests `@WebMvcTest` |

**Décisions prises par l'IA, non demandées explicitement :**
1. omission de Spring Security, dont la présence aurait protégé `/api/health`
   et empêché la vérification du template ;
2. tests fondés sur `@WebMvcTest` avec `DataSource` simulé, exécutables sans
   serveur MySQL actif.

**Problème constaté lors de la vérification :** avertissement dans le journal de
démarrage.

```
WARN org.hibernate.orm.deprecation : HHH90000025: MySQLDialect does not need to
be specified explicitly using 'hibernate.dialect'
```

**Correction appliquée :** suppression de la propriété
`spring.jpa.database-platform`, remplacée par un commentaire explicatif.
Vérification après redémarrage : avertissement absent du journal, endpoint
toujours fonctionnel.

**Tests effectués :**

```
.\mvnw.cmd test
  → Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  → BUILD SUCCESS

.\mvnw.cmd spring-boot:run
  → :: Spring Boot :: (v3.5.16)
  → HikariPool-1 - Added connection / Database version: 8.0.46
  → Tomcat started on port 8080
  → Started PromptlyApplication in 5.072 seconds

Invoke-WebRequest http://localhost:8080/api/health
  → HTTP 200
  → {"status":"UP","application":"Promptly","version":"0.0.1-SNAPSHOT",
     "database":"UP","timestamp":"2026-10-15T18:36:04.019838500Z"}

Requête avec en-tête Origin: http://localhost:4200
  → Access-Control-Allow-Origin: http://localhost:4200
```

---

### P-005 — Génération du frontend

**Date :** 15 octobre 2026
**Objectif :** initialiser Angular, créer la page d'accueil et établir la
communication avec l'API.

**Commandes exécutées :**

```powershell
ng new frontend --routing --style=css --ssr=false --skip-git --zoneless=false --package-manager=npm --ai-config=cursor --defaults
ng generate environments
```

**Résultat obtenu :** quinze fichiers créés ou modifiés (modèle, service, barre
de navigation, pied de page, page d'accueil, routage, configuration, styles
globaux, environnements).

**Problème constaté :** le test `app.spec.ts` généré par Angular CLI contenait
une assertion portant sur le gabarit par défaut, devenue caduque :

```typescript
expect(compiled.querySelector('h1')?.textContent).toContain('Hello, frontend');
```

Le composant racine utilisant désormais le routeur et `HttpClient`, son
instanciation aurait de surcroît échoué faute de fournisseurs.

**Correction appliquée :** réécriture du fichier de test avec `provideRouter`,
`provideHttpClient` et `provideHttpClientTesting`, et assertions conformes au
gabarit réel. Correction du titre de `index.html` (`Frontend` → `Promptly`) et de
la langue du document (`en` → `fr`).

**Tests effectués :**

```
npm run build
  → Initial total: 295.02 kB | 84.95 kB transférés
  → Lazy chunk: home (6.74 kB) — chargement différé effectif
  → Application bundle generation complete. [7.215 seconds]

npm test -- --watch=false --browsers=ChromeHeadless
  → Chrome Headless 155.0.0.0 (Windows 10): Executed 2 of 2 SUCCESS
  → TOTAL: 2 SUCCESS

npm start
  → Local: http://localhost:4200/

Affichage dans le navigateur
  → API : UP | BASE DE DONNEES : UP | Promptly 0.0.1-SNAPSHOT
  → Horodatage : 15/10/2026 19:36:32
```

**Analyse :** l'IA a correctement adopté les conventions d'Angular 20 (signaux,
syntaxe `@if` / `@for`, nommage sans suffixe `.component`) et centralisé l'URL de
l'API dans les fichiers d'environnement. Elle n'a toutefois pas contrôlé
spontanément le fichier de test généré par l'outil tiers.

---

## Phase 1 — Authentification et gestion des utilisateurs

### P-006 — Conception et implémentation du module d'authentification

**Date :** 15 octobre 2026
**Objectif :** concevoir puis réaliser l'inscription, la connexion JWT, le profil
et l'administration des comptes.

**Décisions prises avant implémentation :**
- stockage du jeton dans `localStorage` (limite XSS documentée) ;
- tout nouveau compte est `ETUDIANT` ; seul un `ADMIN` peut promouvoir ;
- premier administrateur créé au démarrage via variables d'environnement ;
- périmètre complet (noyau + page d'administration).

**Résultat obtenu :** backend Spring Security 6 (JWT JJWT 0.13.0, BCrypt) et
frontend Angular (formulaires réactifs, intercepteur, gardes de route).

**Problèmes constatés et corrections :**

1. **BOM UTF-8** (`illegal character: '\ufeff'`). `Set-Content -Encoding UTF8`
   de PowerShell a écrit un BOM que le compilateur Java refuse. Correction :
   réécriture du fichier en UTF-8 sans BOM.
2. **`@WebMvcTest` + filtre JWT.** `JwtAuthenticationFilter` annoté
   `@Component` était chargé dans le contexte réduit des tests de
   `HealthController`, sans le `JwtService` correspondant. Correction : le
   filtre n'est plus un bean, il est instancié dans `SecurityConfig`.
3. **Protection du dernier administrateur faussée.** Les variables
   d'environnement `PROMPTLY_ADMIN_*` ont une priorité supérieure à
   `application-test.properties`. `AdminInitializer` créait donc un second
   administrateur pendant les tests. Correction : `@Profile("!test")` sur
   l'initialiseur ; les tests créent eux-mêmes l'administrateur dont ils ont
   besoin.

**Tests effectués :**

```
.\mvnw.cmd test
  → Tests run: 31, Failures: 0, Errors: 0 — BUILD SUCCESS

npm test -- --watch=false --browsers=ChromeHeadless
  → TOTAL: 2 SUCCESS

npm run build
  → Application bundle generation complete (chunks login, register, profile, users)

GET  /api/health              → 200, database UP
GET  /api/auth/me (sans jeton) → 401
POST /api/auth/register        → 201, role ETUDIANT
POST /api/auth/login           → 200, jeton JWT (3 segments)
GET  /api/users (ETUDIANT)     → 403
GET  /api/users (ADMIN)        → 200, liste paginée
Affichage /admin/utilisateurs  → 2 comptes, bouton Desactiver grisé sur le compte courant
```

**Analyse critique :** l'IA a produit une structure de sécurité cohérente
(DTO sans mot de passe, rôle forcé côté serveur, messages 401 génériques).
Les trois corrections n'ont été identifiées que par exécution réelle. La
priorité des variables d'environnement sur les fichiers de profil est un
piège de Spring Boot que le modèle n'a pas anticipé.

---

## Phase 2 — Gestion des cours

### P-007 — Conception et implémentation du catalogue de cours

**Date :** 15 octobre 2026
**Objectif :** CRUD des cours, classement par technologie, rattachement à un
enseignant, visibilité des brouillons.

**Règles retenues :**
- un cours appartient à un unique enseignant (`ENSEIGNANT` ou `ADMIN`) ;
- un étudiant ne voit que les cours publiés (un brouillon renvoie 404, pas 403) ;
- un enseignant crée et modifie uniquement ses cours ; un administrateur gère tout ;
- un enseignant ne peut pas s'attribuer le cours d'un collègue, même en
  transmettant un `instructorId` ;
- un étudiant ne peut pas être désigné comme enseignant.

**Résultat obtenu :** paquetage `course/` (entité, enum `CourseCategory`,
service, contrôleur, DTO) et pages Angular `/cours`, `/cours/nouveau`,
`/cours/:id`, `/cours/:id/modifier`.

**Problèmes constatés :** aucun échec de test après la première exécution
(`CourseServiceTest` + `CourseRulesIntegrationTest` : 18/18).

**Tests effectués :**

```
.\mvnw.cmd test
  → BUILD SUCCESS (suite complète, dont 18 nouveaux tests cours)

POST /api/courses (ADMIN)     → 201, id=1, titre Introduction a Spring Boot
GET  /api/courses (ETUDIANT)  → 1 cours publié
POST /api/courses (ETUDIANT)  → 403
Page /cours (après login)     → carte du cours + filtres de catégorie
Page /cours/1                 → description, objectifs, boutons Modifier / Supprimer
```

**Analyse critique :** le modèle de visibilité (404 sur un brouillon) et le
forçage de l'enseignant côté serveur sont des décisions de sécurité que l'IA
a appliquées de manière cohérente avec le module d'authentification. Elles
devaient toutefois être spécifiées explicitement : l'IA ne les invente pas
correctement si on lui demande seulement un « CRUD de cours ».

### P-020 — Image, PDF et chapitres sur les cours

**Date :** 15 octobre 2026
**Objectif :** enrichir le module cours avec image de couverture, PDF et
chapitres rattaches.

**Prompt exact :**
> « add img to the cours and upload pdf to the cours and chapter to the cours »

**Résultat obtenu :**
- Stockage local (`FileStorageService`, `GET /api/files/**` public)
- Upload image/PDF sur `Course` ; CRUD chapitres sous `/api/courses/{id}/chapters`
  avec PDF optionnel par chapitre
- Frontend : formulaires multipart, aperçu couverture, pages chapitre
  (liste / detail / form)

**Problèmes constatés :** import `AuthService` dupliqué dans `course-list.ts`
(corrige avant build).

**Tests effectués :**

```
.\mvnw.cmd test "-Dtest=ChapterRulesIntegrationTest,CourseServiceTest,SecurityRulesIntegrationTest"
  → BUILD SUCCESS
npx ng build --configuration=development
  → Application bundle generation complete
```

**Analyse critique :** le decouplage chemin DB / fichier disque est adapte a
un projet pedagogique ; les uploads restent hors Git (`uploads/`).

### P-021 — Module evaluations / quiz

**Date :** 15 octobre 2026
**Objectif :** implementer la gestion des quiz (QCM, soumission, scores).

**Prompt exact :**
> « MAKE THE NEXT GESTION »

**Resultat obtenu :**
- Entites `Quiz`, `Question`, `Choice`, `QuizAttempt`
- API `/api/quizzes` (CRUD enseignant/admin, passage etudiant, historique)
- Frontend : liste, formulaire multi-questions, passage, scores
- Les bonnes reponses ne sont exposees qu'aux enseignants / admin

**Tests effectues :**

```
.\mvnw.cmd test "-Dtest=QuizRulesIntegrationTest"  → BUILD SUCCESS
```

### P-022 — Module forum

**Date :** 15 octobre 2026
**Objectif :** implementer le forum (publications, commentaires, moderation).

**Prompt exact :**
> « next gestion »

**Resultat obtenu :**
- Entites `ForumPost`, `ForumComment` ; categories ; verrouillage
- API `/api/forum/posts` (CRUD, commentaires, lock enseignant/admin)
- Frontend : liste, creation, detail + commentaires
- Auteur ou moderateur pour editer/supprimer ; sujet verrouille = plus de commentaires

**Tests effectues :**

```
.\mvnw.cmd test "-Dtest=ForumRulesIntegrationTest"  → BUILD SUCCESS
```

### P-023 — Module reclamations

**Date :** 15 octobre 2026
**Objectif :** implementer le depot et le traitement des reclamations.

**Prompt exact :**
> « good make the next one »

**Resultat obtenu :**
- Entite `Complaint` (categories, statuts OUVERTE/EN_COURS/RESOLUE/REJETEE)
- API `/api/complaints` : depot pour tous, liste filtree, traitement ADMIN
- Frontend : liste, formulaire, detail + panneau de traitement admin
- Visibilite : auteur voit ses demandes ; admin voit tout

**Tests effectues :**

```
.\mvnw.cmd test "-Dtest=ComplaintRulesIntegrationTest"  → BUILD SUCCESS
```

---

## Modèle d'entrée pour les phases suivantes

```markdown
### P-0XX — [Titre de la tâche]

**Date :**
**Objectif :**

**Prompt exact :**
> [texte intégral du prompt]

**Contexte fourni :** [fichiers, erreurs, sorties de commandes]

**Résultat obtenu :** [description ; extraits de code courts]

**Problèmes constatés :** [erreurs, omissions, pratiques obsolètes]

**Prompt de correction :**
> [texte intégral, le cas échéant]

**Résultat corrigé :** [description]

**Modifications appliquées au projet :** [liste des fichiers]

**Tests effectués :** [commandes et sorties réelles]

**Analyse critique :** [apports de l'IA, limites, intervention humaine nécessaire]
```
