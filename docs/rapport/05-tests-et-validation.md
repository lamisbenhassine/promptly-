# 5. Tests et validation

## 5.1 Stratégie de test

La validation s'appuie sur quatre niveaux complémentaires :

| Niveau | Outil | Objet | Mis en œuvre en phase 0 |
|--------|-------|-------|-------------------------|
| Tests unitaires backend | JUnit 5 + Mockito | Logique d'une classe isolée | Oui |
| Tests de couche web | MockMvc (`@WebMvcTest`) | Contrôleurs REST sans serveur réel | Oui |
| Tests unitaires frontend | Karma + Jasmine | Composants et services Angular | Oui |
| Vérification manuelle | PowerShell, navigateur, Postman | Fonctionnement de bout en bout | Oui |
| Tests d'intégration avec base | `@SpringBootTest` + base de test | Persistance réelle | À partir de la phase 1 |

> **Principe appliqué.** Aucun résultat n'est rapporté dans ce chapitre sans
> avoir été effectivement observé. Les sorties de commandes reproduites ci-après
> sont des extraits réels d'exécution du 15 octobre 2026.

## 5.2 Tests automatisés du backend

### 5.2.1 Cas de test

Fichier : `backend/src/test/java/com/promptly/backend/health/HealthControllerTest.java`

| N° | Nom du test | Objectif | Résultat attendu |
|----|-------------|----------|------------------|
| T1 | `shouldReturnUpWhenDatabaseIsReachable` | Vérifier la réponse nominale | `200 OK`, `status=UP`, `application=Promptly`, `database=UP`, `timestamp` présent |
| T2 | `shouldReportDatabaseDownWithoutFailing` | Vérifier la robustesse en cas de panne de la base | `200 OK`, `status=UP`, `database=DOWN: Connection refused` |

### 5.2.2 Choix de conception des tests

Les tests utilisent `@WebMvcTest` associé à un `DataSource` simulé
(`@MockitoBean`) plutôt que `@SpringBootTest`. Trois conséquences :

- **aucun serveur MySQL n'est requis** : les tests s'exécutent même si la base
  est arrêtée, ce qui les rend utilisables en intégration continue ;
- **la simulation d'une panne est possible** : le test T2 déclenche
  volontairement une `SQLException`, situation irréalisable avec une base réelle ;
- **l'exécution est rapide** : seule la couche web est chargée.

### 5.2.3 Résultat d'exécution

Commande :

```powershell
cd backend
.\mvnw.cmd test
```

Sortie observée :

```
[INFO]  T E S T S
[INFO] Running com.promptly.backend.health.HealthControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.18 s
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Conclusion : 2 tests exécutés, 2 réussis, 0 échec.**

[À INSÉRER : Capture de l'exécution des tests backend — `backend_tests_succes.png`]

## 5.3 Tests automatisés du frontend

### 5.3.1 Cas de test

Fichier : `frontend/src/app/app.spec.ts`

| N° | Nom du test | Objectif |
|----|-------------|----------|
| T3 | `should create the app` | Vérifier l'instanciation du composant racine |
| T4 | `should render the Promptly navbar and footer` | Vérifier la présence de `app-navbar`, `app-footer` et du nom « Promptly » |

### 5.3.2 Résultat d'exécution

Commande :

```powershell
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless
```

Sortie observée :

```
Chrome Headless 155.0.0.0 (Windows 10): Executed 2 of 2 SUCCESS (0.155 secs / 0.13 secs)
TOTAL: 2 SUCCESS
```

**Conclusion : 2 tests exécutés, 2 réussis, 0 échec.**

[À INSÉRER : Capture de l'exécution des tests frontend — `frontend_tests_succes.png`]

## 5.4 Vérification de la compilation

### 5.4.1 Backend

La commande `.\mvnw.cmd test` inclut la phase de compilation. Résultat :
`BUILD SUCCESS`.

### 5.4.2 Frontend

Commande :

```powershell
cd frontend
npm run build
```

Sortie observée :

```
Initial chunk files   | Names         |  Raw size | Estimated transfer size
chunk-G6YHDXSG.js     | -             | 170.38 kB |                49.79 kB
main-XTDNVVSW.js      | main          |  89.11 kB |                22.89 kB
polyfills-5CFQRCPP.js | polyfills     |  34.59 kB |                11.33 kB
styles-6XVXHCBR.css   | styles        | 947 bytes |               947 bytes
                      | Initial total | 295.02 kB |                84.95 kB

Lazy chunk files      | Names         |  Raw size | Estimated transfer size
chunk-3QSUOEWU.js     | home          |   6.74 kB |                 2.30 kB

Application bundle generation complete. [7.215 seconds]
```

Deux observations :

- la compilation aboutit sans erreur ni avertissement ;
- la page d'accueil apparaît en **chargement différé** (`Lazy chunk files`), ce
  qui confirme que la stratégie `loadComponent` déclarée dans `app.routes.ts` est
  effective. Le fichier initial reste ainsi limité à 295 ko, soit 85 ko
  transférés après compression.

[À INSÉRER : Capture de la compilation du frontend — `frontend_build_succes.png`]

## 5.5 Vérification manuelle du backend

### 5.5.1 Démarrage du serveur

Commande :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Extraits du journal observé :

```
 :: Spring Boot ::               (v3.5.16)
Starting PromptlyApplication using Java 17.0.2
Bootstrapping Spring Data JPA repositories in DEFAULT mode.
Finished Spring Data repository scanning in 18 ms. Found 0 JPA repository interfaces.
Tomcat initialized with port 8080 (http)
Hibernate ORM core version 6.6.53.Final
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@7abc6992
HikariPool-1 - Start completed.
	Database version: 8.0.46
Tomcat started on port 8080 (http) with context path '/'
Started PromptlyApplication in 5.072 seconds (process running for 5.655)
```

Ce journal permet de valider quatre points :

| Point vérifié | Preuve dans le journal |
|---------------|------------------------|
| Version de Spring Boot effective | `:: Spring Boot :: (v3.5.16)` |
| Version de Java effective | `using Java 17.0.2` |
| Connexion à MySQL établie | `HikariPool-1 - Added connection` et `Database version: 8.0.46` |
| Serveur à l'écoute | `Tomcat started on port 8080` |

La mention `Found 0 JPA repository interfaces` confirme par ailleurs qu'aucune
entité métier n'a été introduite, conformément au périmètre de la phase 0.

[À INSÉRER : Capture de la console de démarrage du backend — `backend_demarrage_console.png`]

### 5.5.2 Appel de l'endpoint de vérification

Commande :

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/health" -UseBasicParsing
```

Résultat observé :

```
HTTP 200
{"status":"UP","application":"Promptly","version":"0.0.1-SNAPSHOT","database":"UP","timestamp":"2026-10-15T18:36:04.019838500Z"}
```

| Champ | Valeur obtenue | Conforme |
|-------|----------------|----------|
| Code HTTP | `200` | Oui |
| `status` | `UP` | Oui |
| `application` | `Promptly` | Oui |
| `version` | `0.0.1-SNAPSHOT` | Oui |
| `database` | `UP` | Oui |
| `timestamp` | `2026-10-15T18:36:04.019838500Z` | Oui (ISO 8601, UTC) |

[À INSÉRER : Capture de l'appel de l'endpoint dans Postman — `health_endpoint_postman.png`]

### 5.5.3 Vérification de la configuration CORS

Commande :

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/health" -Headers @{Origin="http://localhost:4200"} -UseBasicParsing
```

Résultat observé :

```
Access-Control-Allow-Origin: http://localhost:4200
```

L'en-tête attendu est présent, ce qui confirme que le navigateur autorisera les
requêtes émises depuis le frontend Angular.

## 5.6 Vérification manuelle du frontend

### 5.6.1 Démarrage du serveur de développement

Commande :

```powershell
cd frontend
npm start
```

Sortie observée :

```
Application bundle generation complete. [5.535 seconds]
Watch mode enabled. Watching for file changes...
  ➜  Local:   http://localhost:4200/
```

### 5.6.2 Affichage de la page d'accueil

Page ouverte à l'adresse `http://localhost:4200`. Éléments effectivement
observés :

| Élément | Observation |
|---------|-------------|
| Titre de l'onglet | `Promptly - Accueil` (défini par la propriété `title` de la route) |
| Barre de navigation | Logo « `</>` Promptly », lien « Accueil » actif ; liens Cours, Quiz, Forum, Réclamations grisés |
| Section d'accueil | Titre « Apprenez a programmer avec **Promptly** » |
| État de l'API | Badge vert `UP` |
| État de la base | Badge vert `UP` |
| Application | `Promptly 0.0.1-SNAPSHOT` |
| Horodatage | `15/10/2026 19:36:32` (format français appliqué par `DatePipe`) |
| Modules prévus | Six cartes affichant la mention « À venir » |
| Pied de page | « Promptly — plateforme e-learning dediee a la programmation » |

**Cette observation constitue la validation de bout en bout de la phase 0** :
l'affichage des badges `UP` démontre que le navigateur a exécuté l'application
Angular, que celle-ci a émis une requête HTTP vers le port 8080, que la requête
n'a pas été bloquée par la politique d'origine croisée, que Spring Boot a traité
l'appel, que la connexion MySQL a été validée et que la réponse JSON a été
correctement désérialisée et affichée.

[À INSÉRER : Capture de la page d'accueil — `frontend_page_accueil.png`]

### 5.6.3 Vérification de l'adaptation responsive

La page a été réaffichée avec une émulation de viewport mobile (390 × 844 px,
facteur d'échelle 2) au moyen des outils de développement du navigateur.
Comportement observé :

| Élément | Comportement en largeur réduite |
|---------|--------------------------------|
| Barre de navigation | Passage en disposition verticale ; le logo se place au-dessus des liens |
| Liens de navigation | Retour à la ligne automatique sur deux rangées |
| Titre de la section d'accueil | Réduction de la taille de police (`clamp()`) et retour à la ligne |
| Carte d'état du backend | Passage de quatre colonnes à une seule colonne |
| Cartes des modules | Passage de trois colonnes à une seule colonne |

Aucun débordement horizontal ni chevauchement n'a été constaté.

[À INSÉRER : Capture du rendu mobile — `frontend_responsive_mobile.png`]

## 5.7 Anomalies détectées et corrigées

| N° | Anomalie | Détection | Correction | Vérification |
|----|----------|-----------|------------|--------------|
| A1 | Angular CLI 18 incompatible avec Node.js 24 (`Node: 24.13.1 (Unsupported)`) | `ng version`, avant toute écriture de code | Mise à niveau vers Angular CLI 20 | `ng version` → `Angular CLI: 20.3.40`, avertissement disparu |
| A2 | Version de Spring Boot recommandée indisponible | Interrogation de Spring Initializr et de Maven Central | Version 3.5.16 retenue, `pom.xml` rédigé manuellement | `BUILD SUCCESS` et `:: Spring Boot :: (v3.5.16)` au démarrage |
| A3 | Avertissement `HHH90000025` : dialecte Hibernate déclaré inutilement | Journal de démarrage du backend | Suppression de la propriété `spring.jpa.database-platform` | Avertissement absent du journal après redémarrage ; endpoint toujours fonctionnel |
| A4 | Test `app.spec.ts` généré par Angular CLI portant sur le gabarit par défaut (`Hello, frontend`) | Lecture du fichier généré | Réécriture avec les fournisseurs nécessaires et des assertions conformes | `TOTAL: 2 SUCCESS` |

## 5.8 Récapitulatif de la validation de la phase 0

| N° | Critère de validation | Méthode | Résultat |
|----|----------------------|---------|----------|
| V1 | Le backend compile | `.\mvnw.cmd test` | **Validé** — `BUILD SUCCESS` |
| V2 | Les tests backend réussissent | `.\mvnw.cmd test` | **Validé** — 2/2 |
| V3 | Le backend démarre | `.\mvnw.cmd spring-boot:run` | **Validé** — port 8080 |
| V4 | La connexion MySQL fonctionne | journal + champ `database` | **Validé** — `UP`, MySQL 8.0.46 |
| V5 | L'endpoint `/api/health` répond | `Invoke-WebRequest` | **Validé** — `HTTP 200` |
| V6 | CORS est configuré | requête avec en-tête `Origin` | **Validé** — en-tête présent |
| V7 | Le frontend compile | `npm run build` | **Validé** — 295,02 ko |
| V8 | Les tests frontend réussissent | `npm test` | **Validé** — 2/2 |
| V9 | Le frontend démarre | `npm start` | **Validé** — port 4200 |
| V10 | La page d'accueil s'affiche | navigateur | **Validé** |
| V11 | Angular communique avec Spring Boot | badges d'état sur la page | **Validé** — `API: UP`, `BASE: UP` |
| V12 | Aucun secret n'est présent dans le dépôt | recherche automatisée du mot de passe dans les 64 fichiers texte indexés par Git | **Validé** — aucune occurrence |
| V13 | La mise en page s'adapte aux petits écrans | émulation mobile 390 × 844 px | **Validé** — aucun débordement |

**Bilan : les treize critères de validation de la phase 0 sont satisfaits.**

## 5.9 Validation du module 1 — Authentification

Les tests backend portent désormais sur 31 cas (2 de la phase 0 + 8 unitaires
de `UserService` + 21 d'intégration de la chaîne de sécurité).

Commande :

```powershell
cd backend
.\mvnw.cmd test
```

Résultat observé le 15 octobre 2026 :

```
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Vérifications manuelles complémentaires :

| N° | Critère | Résultat observé |
|----|---------|------------------|
| A1 | `/api/health` reste public | `200`, `database: UP` |
| A2 | `/api/auth/me` sans jeton | `401` |
| A3 | Inscription | `201`, rôle forcé à `ETUDIANT` |
| A4 | Connexion | jeton JWT à 3 segments |
| A5 | Liste des utilisateurs en ETUDIANT | `403` |
| A6 | Liste des utilisateurs en ADMIN | `200` |
| A7 | Page `/admin/utilisateurs` | 2 comptes affichés ; bouton « Désactiver » grisé sur le compte courant |
| A8 | Compilation frontend | succès, chunks `login`, `register`, `profile`, `users` |

Trois anomalies ont été détectées puis corrigées pendant cette phase
(BOM UTF-8, filtre JWT dans `@WebMvcTest`, priorité des variables
d'environnement sur `application-test.properties`). Le détail figure dans
`journal-prompts.md` (entrée P-006).

[À INSÉRER : Capture des 31 tests backend — `auth_tests_backend.png`]
[À INSÉRER : Capture de la page d'administration — `auth_page_admin.png`]
[À INSÉRER : Capture du 403 dans Postman — `auth_postman_403.png`]

## 5.10 Validation du module 2 — Cours

18 tests automatisés ont été ajoutés (`CourseServiceTest` : 6 ; `CourseRulesIntegrationTest` : 12).

Résultat de la suite complète le 15 octobre 2026 : `BUILD SUCCESS`.

Vérifications manuelles :

| N° | Critère | Résultat observé |
|----|---------|------------------|
| C1 | Création par un administrateur | `201`, cours `Introduction a Spring Boot` |
| C2 | Catalogue étudiant | 1 cours publié visible |
| C3 | Création par un étudiant | `403` |
| C4 | Page `/cours` | filtres par catégorie + carte du cours |
| C5 | Page `/cours/1` | description, objectifs, actions Modifier / Supprimer pour l'admin |

[À INSÉRER : Capture du catalogue — `cours_catalogue.png`]
[À INSÉRER : Capture de la fiche — `cours_detail.png`]
[À INSÉRER : Capture du 403 étudiant — `cours_postman_403.png`]

## 5.11 Limites de la validation actuelle

Par souci d'exactitude, les points suivants n'ont **pas** été vérifiés :

- aucun test d'intégration avec une base de données réelle (sans objet en
  l'absence d'entité) ;
- aucun test de charge ni mesure de performance ;
- aucune vérification sur un navigateur autre que Chrome ;
- le rendu responsive a été contrôlé **par émulation** dans les outils de
  développement, mais sur **aucun appareil mobile physique** ;
- aucun test de sécurité, le module d'authentification n'étant pas réalisé ;
- aucun test d'accessibilité.
