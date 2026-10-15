# 6. Index des captures d'écran

## 6.1 Avertissement

> **AUCUNE CAPTURE D'ÉCRAN N'A ÉTÉ RÉALISÉE À CE JOUR.**
>
> Ce chapitre constitue une **liste de contrôle** des captures à effectuer. Les
> dossiers de `docs/screenshots/` existent mais sont vides. Chaque entrée précise
> le nom de fichier à utiliser, l'élément à cadrer et ce que la capture doit
> démontrer.
>
> Les emplacements dans le rapport sont marqués par la mention
> `[À INSÉRER : ...]`. Ces marqueurs devront être remplacés par les images une
> fois celles-ci réalisées.

## 6.2 Convention de nommage

| Règle | Exemple |
|-------|---------|
| Minuscules, mots séparés par un tiret bas | `health_endpoint_postman.png` |
| Préfixe indiquant le sujet | `template_`, `backend_`, `frontend_`, `ollama_` |
| Format PNG | — |
| Dossier correspondant au module | `docs/screenshots/01-template/` |

## 6.3 Phase 0 — Captures à réaliser

### 6.3.1 Utilisation de l'IA comme assistant de développement

Dossier : `docs/screenshots/08-ia-developpement/`

| Figure | Nom de fichier | À cadrer | Démontre |
|--------|----------------|----------|----------|
| 6.1 | `template_prompt_initial.png` | Le prompt complet du cahier des charges dans Cursor | Le périmètre et les contraintes réellement soumis à l'IA |
| 6.2 | `template_inspection_environnement.png` | La réponse de l'IA listant les versions détectées, avec la ligne `Node: 24.13.1 (Unsupported)` | La détection automatisée de l'incompatibilité avant toute écriture de code (anomalie A1) |
| 6.3 | `template_decisions_arbitrage.png` | Les trois questions posées par l'IA et les réponses fournies | Le respect de la consigne d'attente de validation humaine |
| 6.4 | `springboot_version_proposition_initiale.png` | Le passage recommandant Spring Boot 3.4.x | Le résultat initial, avant correction (anomalie A2) |
| 6.5 | `springboot_version_verification_mavencentral.png` | La sortie de la commande interrogeant Maven Central (liste des versions 3.5.x) | La vérification par une source externe, qui a révélé l'erreur |
| 6.6 | `springboot_version_correction.png` | Le passage justifiant le passage à 3.5.16 | La correction appliquée et sa justification |
| 6.7 | `hibernate_warning_detection.png` | La ligne `HHH90000025` dans la console du backend | La détection de l'anomalie A3 par exécution réelle |
| 6.8 | `hibernate_warning_correction.png` | Le fichier `application.properties` corrigé, et la console après redémarrage sans l'avertissement | La correction de l'anomalie A3 et sa vérification |
| 6.9 | `angular_test_correction.png` | L'ancien test (`Hello, frontend`) et le test réécrit, côte à côte | La correction de l'anomalie A4 |
| 6.10 | `securite_variables_environnement.png` | Les lignes `${PROMPTLY_DB_PASSWORD:}` de `application.properties` et la section secrets de `.gitignore` | L'absence de mot de passe dans le code versionné |

### 6.3.2 Template et fondation technique

Dossier : `docs/screenshots/01-template/`

| Figure | Nom de fichier | À cadrer | Démontre |
|--------|----------------|----------|----------|
| 6.11 | `structure_projet_vscode.png` | L'explorateur de fichiers déplié sur `backend/`, `frontend/` et `docs/` | La structure de projet effectivement créée |
| 6.12 | `backend_demarrage_console.png` | La console affichant la bannière `:: Spring Boot :: (v3.5.16)` jusqu'à `Started PromptlyApplication` | Le démarrage réussi, la version de Spring Boot et la connexion MySQL (critères V3 et V4) |
| 6.13 | `health_endpoint_postman.png` | Postman : requête `GET http://localhost:8080/api/health`, code `200`, corps JSON | Le fonctionnement de l'API (critère V5) |
| 6.14 | `frontend_page_accueil.png` | La page complète à `http://localhost:4200`, badges `UP` visibles | La validation de bout en bout (critères V10 et V11) |
| 6.15 | `frontend_appel_api_devtools.png` | Onglet Réseau des outils de développement, requête `health` sélectionnée, code `200` et onglet Réponse | La communication HTTP effective entre Angular et Spring Boot |
| 6.16 | `mysql_workbench_base_vide.png` | MySQL Workbench affichant `promptly_db` sans table | L'existence de la base et l'absence d'entité en phase 0 |
| 6.17 | `frontend_responsive_mobile.png` | La page en mode appareil mobile (outils de développement, largeur ≈ 390 px) | L'adaptation responsive de la mise en page |

### 6.3.3 Tests

Dossier : `docs/screenshots/10-tests/`

| Figure | Nom de fichier | À cadrer | Démontre |
|--------|----------------|----------|----------|
| 6.18 | `backend_tests_succes.png` | La sortie `Tests run: 2, Failures: 0, Errors: 0` et `BUILD SUCCESS` | Le succès des tests backend (critère V2) |
| 6.19 | `frontend_tests_succes.png` | La sortie `TOTAL: 2 SUCCESS` | Le succès des tests frontend (critère V8) |
| 6.20 | `frontend_build_succes.png` | Le tableau des tailles de fichiers, avec la ligne `Lazy chunk files` | La compilation de production et le chargement différé (critère V7) |
| 6.21 | `cors_verification.png` | La commande PowerShell et l'en-tête `Access-Control-Allow-Origin` | La configuration CORS (critère V6) |

## 6.4 Procédure de capture recommandée

1. **Préparation.** Fermer les fenêtres et onglets sans rapport avec le sujet,
   afin de ne pas surcharger l'image.
2. **Cadrage.** Capturer la fenêtre concernée plutôt que l'écran entier, en
   veillant à ce que le texte demeure lisible (`Alt + Impr. écran` sous Windows,
   ou l'outil Capture d'écran).
3. **Preuve de contexte.** Conserver visibles les éléments permettant
   d'identifier la source : URL dans le navigateur, invite de commande, nom du
   fichier dans l'éditeur.
4. **Enregistrement.** Nommer le fichier conformément au tableau ci-dessus et le
   placer dans le dossier indiqué.
5. **Insertion.** Remplacer le marqueur `[À INSÉRER : ...]` correspondant dans le
   chapitre concerné par la syntaxe suivante :

   ```markdown
   ![Figure 6.13 — Appel de l'endpoint /api/health dans Postman](../screenshots/01-template/health_endpoint_postman.png)

   *Figure 6.13 — Appel de l'endpoint `/api/health` dans Postman. La réponse
   `200 OK` et le champ `"database":"UP"` confirment que l'API répond et que la
   connexion MySQL est opérationnelle.*
   ```

6. **Légende.** Chaque figure doit comporter un numéro, un titre et une phrase
   expliquant ce qu'elle démontre.

## 6.5 Phase 1 — Authentification

Dossier : `docs/screenshots/02-authentification/`

| Figure | Nom de fichier | À cadrer | Démontre |
|--------|----------------|----------|----------|
| 6.22 | `auth_page_inscription.png` | Formulaire `/inscription` | Validation côté client (messages d'erreur) |
| 6.23 | `auth_page_connexion.png` | Formulaire `/connexion` | Page de connexion |
| 6.24 | `auth_navbar_connecte.png` | Barre après connexion administrateur | Nom, rôle, lien Utilisateurs, bouton Déconnexion |
| 6.25 | `auth_page_profil.png` | Page `/profil` | Modification du profil et du mot de passe |
| 6.26 | `auth_page_admin.png` | Page `/admin/utilisateurs` | Liste, sélecteur de rôle, bouton Désactiver grisé sur le compte courant |
| 6.27 | `auth_postman_login.png` | Postman : `POST /api/auth/login` | Jeton JWT renvoyé |
| 6.28 | `auth_postman_403.png` | Postman : `GET /api/users` avec un jeton ETUDIANT | Refus 403 |
| 6.29 | `auth_tests_backend.png` | Sortie `Tests run: 31, Failures: 0` | Validation automatisée du module |

Dossier complémentaire : `docs/screenshots/08-ia-developpement/`

| Figure | Nom de fichier | À cadrer | Démontre |
|--------|----------------|----------|----------|
| 6.30 | `auth_correction_bom.png` | Erreur `\ufeff` puis fichier réécrit sans BOM | Correction d'un artefact PowerShell |
| 6.31 | `auth_correction_admin_initializer.png` | Annotation `@Profile("!test")` et commentaire associé | Priorité des variables d'environnement sur `application-test.properties` |

## 6.6 Phase 2 — Cours

Dossier : `docs/screenshots/03-cours/`

| Figure | Nom de fichier | À cadrer | Démontre |
|--------|----------------|----------|----------|
| 6.32 | `cours_catalogue.png` | `/cours` avec filtres et au moins une carte | Catalogue et classement par technologie |
| 6.33 | `cours_detail.png` | `/cours/1` avec description et objectifs | Fiche pédagogique |
| 6.34 | `cours_formulaire.png` | `/cours/nouveau` | Formulaire de création |
| 6.35 | `cours_postman_403.png` | `POST /api/courses` avec un jeton ETUDIANT | Refus de création |
| 6.36 | `cours_brouillon_404.png` | `GET /api/courses/{id}` d'un brouillon en ETUDIANT | 404 plutôt que 403 |
| 6.37 | `cours_tests_backend.png` | Sortie des 18 tests cours | Validation automatisée |

## 6.7 Captures des phases ultérieures

Les dossiers suivants sont créés mais vides. Leur contenu sera défini au début de
chaque module.

| Dossier | Module correspondant |
|---------|---------------------|
| `04-chapitres/` | Gestion des chapitres |
| `05-evaluations/` | Quiz et résultats |
| `06-forum/` | Publications et commentaires |
| `07-reclamations/` | Dépôt et traitement des réclamations |
| `09-ia-application/` | Modèle en exécution, requêtes, réponses, résultat dans Promptly |
