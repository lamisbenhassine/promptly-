# 3. Utilisation de l'IA comme assistant de développement

## 3.1 Objet du chapitre

Ce chapitre documente l'utilisation d'outils d'intelligence artificielle
générative **pendant la production du logiciel**. L'IA y est considérée comme un
outil de développement, au même titre qu'un environnement de développement
intégré ou un gestionnaire de dépendances. Elle **ne fait pas partie du
livrable** : l'IA intégrée au produit fini est traitée séparément au chapitre 4.

## 3.2 Outil utilisé

| Élément | Valeur |
|---------|--------|
| Outil | Cursor (environnement de développement assisté par IA) |
| Mode d'utilisation | Agent avec accès au système de fichiers et au terminal |
| Modèle | Claude (Anthropic) — l'identifiant exact de version n'est pas exposé par l'interface |
| Contexte fourni | Répertoire de travail complet, sortie des commandes du terminal, contenu des fichiers lus |
| Période | Phase 0 — 15 octobre 2026 |

> **Précision méthodologique.** L'interface de Cursor n'affiche pas
> systématiquement l'identifiant précis de la version du modèle. Cette
> information est donc rapportée telle qu'elle est disponible, sans extrapolation.

## 3.3 Méthodologie adoptée

Le mode de collaboration retenu repose sur trois principes :

1. **Validation humaine avant écriture.** L'agent devait inspecter
   l'environnement et soumettre une proposition avant toute création de fichier.
   Les décisions structurantes (version du framework, organisation des
   paquetages, gestion des secrets) ont fait l'objet d'un arbitrage explicite.
2. **Vérification systématique.** Aucune affirmation de bon fonctionnement n'a
   été acceptée sans exécution réelle : compilation, exécution des tests, appel
   effectif de l'endpoint, affichage de la page dans un navigateur.
3. **Progression par phases.** Le périmètre de chaque étape a été délimité à
   l'avance afin d'éviter la production de code non maîtrisé.

## 3.4 Tâche 1 — Inspection de l'environnement et proposition d'architecture

### 3.4.1 Objectif

Déterminer l'état initial du poste de développement, vérifier la présence et la
compatibilité des outils requis, puis proposer une structure de projet.

### 3.4.2 Prompt initial

Le prompt initial était un cahier des charges complet (environ 300 lignes)
définissant le contexte du projet, les sept modules fonctionnels, la pile
technologique imposée, le déroulement obligatoire par phases, les exigences de
documentation de l'IA et les règles de qualité. Son instruction opérationnelle
était la suivante :

> « Begin with **Phase 0: Project Template and Foundation**. First inspect the
> workspace and report what already exists. Then propose the folder structure,
> the technologies and compatible versions, the initialization commands, and the
> planned files. Do not implement the complete project. [...] Wait for my
> confirmation before making the initial structural changes. »

Le prompt comportait également une contrainte explicite de non-fabrication :

> « Do not invent prompts, generated results, tests, screenshots, or AI
> contributions. »

### 3.4.3 Contexte fourni à l'IA

- Répertoire de travail `c:\Users\lpatron\OneDrive\Desktop\ia` (vide) ;
- système d'exploitation Windows 10 et interpréteur PowerShell ;
- accès en exécution au terminal.

### 3.4.4 Résultat obtenu

L'agent a exécuté des commandes de diagnostic (`java -version`, `mvn -v`,
`node -v`, `ng version`, `mysql --version`, `Get-Service *mysql*`,
`Get-NetTCPConnection`) et a produit le constat suivant :

| Outil | Version détectée | Conformité |
|-------|------------------|-----------|
| JDK | 17.0.2 LTS | Conforme |
| Maven | 3.9.11 | Conforme |
| Node.js | 24.13.1 | **Incompatible avec Angular 18** |
| npm | 11.8.0 | Conforme |
| Angular CLI | 18.2.21 | À mettre à niveau |
| MySQL Server | 8.0.46, service actif, port 3306 à l'écoute | Conforme |
| Git | 2.47.0 | Installé, aucun dépôt initialisé |

**Anomalie identifiée par l'IA.** La commande `ng version` renvoyait
explicitement `Node: 24.13.1 (Unsupported)`. Angular 18 ne prend en charge que
Node.js 18, 20 et 22. Cette incompatibilité, non anticipée dans le cahier des
charges, aurait provoqué des erreurs de compilation difficiles à diagnostiquer.
L'agent a signalé le problème **avant** toute génération de code et a proposé
trois options (mise à niveau d'Angular CLI, installation de Node.js 22 en
parallèle, maintien de la configuration incompatible).

### 3.4.5 Analyse critique

| Aspect | Évaluation |
|--------|-----------|
| Utile | Le diagnostic automatisé de l'environnement a détecté une incompatibilité que l'analyse manuelle du cahier des charges n'aurait pas révélée |
| Utile | Le respect de la consigne d'attente de validation a été effectif : aucun fichier n'a été créé avant l'arbitrage |
| Limite | La détection de cinq services MySQL (dont quatre arrêtés, résidus d'installations antérieures) a nécessité une interprétation humaine pour être écartée |
| Intervention humaine | Arbitrage sur la stratégie de résolution de l'incompatibilité Node.js/Angular |

**Captures à réaliser :** `template_prompt_initial.png`,
`template_inspection_environnement.png` (voir chapitre 6).

## 3.5 Tâche 2 — Choix de la version de Spring Boot (correction d'une proposition erronée)

### 3.5.1 Objectif

Déterminer une version de Spring Boot compatible avec Java 17 et avec
l'intégration ultérieure d'un modèle de langage.

### 3.5.2 Résultat initial proposé par l'IA

Dans sa proposition d'architecture, l'agent a d'abord recommandé **Spring Boot
3.4.x**, en justifiant ce choix par la compatibilité de Spring AI 1.0 avec cette
ligne de versions.

### 3.5.3 Problème constaté

Cette recommandation a été formulée **sans vérification préalable des versions
réellement disponibles**. L'interrogation de l'API de Spring Initializr a révélé
un écart majeur :

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

Spring Initializr ne proposait plus **aucune** version 3.x. La proposition
initiale de l'IA reposait donc sur une hypothèse implicite, héritée de ses
données d'entraînement, et non sur l'état effectif de l'écosystème.

### 3.5.4 Correction appliquée

La vérification a été étendue au dépôt Maven Central, source de vérité pour les
artefacts publiés :

```powershell
$x = [xml](Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml" -UseBasicParsing).Content
$x.metadata.versioning.versions.version | Where-Object { $_ -like "3.5.*" }
```

La dernière version publiée de la ligne 3.5 s'est avérée être la **3.5.16**. La
recommandation a été révisée en conséquence, avec une justification corrigée :

| Version | Décision | Motif |
|---------|----------|-------|
| 3.4.x | Écartée | Supplantée par la ligne 3.5, plus récente et également compatible avec Spring AI |
| **3.5.16** | **Retenue** | Dernière version de la ligne 3.x ; Spring AI 1.1 y est stabilisé ; documentation abondante |
| 4.0.8+ | Écartée | Support de Spring AI encore en version *milestone* ; risque de changement d'API en cours de projet |

Spring Initializr ne pouvant plus générer de projet 3.x, le fichier `pom.xml` a
été **rédigé manuellement**, puis le *wrapper* Maven ajouté par la commande
`mvn -N wrapper:wrapper`.

### 3.5.5 Analyse critique

Cet épisode illustre une limite caractéristique des modèles de langage : la
**connaissance figée au moment de l'entraînement**. Le modèle a énoncé une
version plausible et cohérente avec sa justification, mais désalignée de l'état
réel du dépôt. L'erreur n'a pas été détectée par raisonnement interne, mais par
**interrogation d'une source externe**.

**Enseignement retenu pour la suite du projet :** toute version de dépendance
proposée par l'IA doit être vérifiée auprès du dépôt officiel avant d'être
inscrite dans un fichier de configuration.

**Captures à réaliser :** `springboot_version_proposition_initiale.png`,
`springboot_version_verification_mavencentral.png`,
`springboot_version_correction.png` (voir chapitre 6).

## 3.6 Tâche 3 — Génération du backend et de l'endpoint de vérification

### 3.6.1 Objectif

Produire un projet Spring Boot fonctionnel exposant un endpoint `/api/health`
permettant de vérifier que l'API répond et que MySQL est joignable.

### 3.6.2 Résultat obtenu

Fichiers générés par l'IA :

| Fichier | Contenu |
|---------|---------|
| `pom.xml` | Parent 3.5.16, Java 17, six dépendances |
| `PromptlyApplication.java` | Classe de démarrage |
| `config/CorsConfig.java` | Autorisation de l'origine `http://localhost:4200` |
| `health/HealthController.java` | Endpoint `GET /api/health` |
| `health/HealthResponse.java` | `record` de réponse |
| `application.properties` | Configuration MySQL, JPA, CORS |
| `HealthControllerTest.java` | Deux tests `@WebMvcTest` |

Deux décisions de conception prises par l'IA méritent d'être relevées, car elles
n'étaient pas explicitement demandées :

1. **Omission volontaire de Spring Security.** L'agent a justifié cette
   omission : la présence de cette dépendance active une protection automatique
   de toutes les routes, et `/api/health` renverrait une page de connexion, ce
   qui empêcherait la vérification du template.
2. **Isolation des tests par rapport à la base de données.** Les tests utilisent
   `@WebMvcTest` avec un `DataSource` simulé (`@MockitoBean`) plutôt que
   `@SpringBootTest`. Ils peuvent donc s'exécuter même si le serveur MySQL est
   arrêté.

### 3.6.3 Problème constaté lors de la vérification

Le démarrage a réussi, mais le journal d'Hibernate comportait un avertissement :

```
WARN  org.hibernate.orm.deprecation : HHH90000025: MySQLDialect does not need to
be specified explicitly using 'hibernate.dialect' (remove the property setting
and it will be selected by default)
```

L'IA avait ajouté la propriété `spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect`.
Cette pratique, courante dans les tutoriels anciens, est devenue superflue :
Hibernate 6 déduit le dialecte de la connexion JDBC.

### 3.6.4 Correction appliquée

La propriété a été supprimée et remplacée par un commentaire explicatif :

```properties
# Le dialecte n'est pas declare explicitement : Hibernate le deduit de la
# connexion JDBC (le declarer provoque l'avertissement HHH90000025).
```

Après redémarrage, l'avertissement a disparu du journal, l'endpoint continuant
de répondre correctement.

### 3.6.5 Tests effectués

| Test | Commande | Résultat observé |
|------|----------|------------------|
| Compilation et tests unitaires | `.\mvnw.cmd test` | `Tests run: 2, Failures: 0, Errors: 0` — `BUILD SUCCESS` |
| Démarrage du serveur | `.\mvnw.cmd spring-boot:run` | `Tomcat started on port 8080` — `Started PromptlyApplication in 5.072 seconds` |
| Connexion à MySQL | journal de démarrage | `HikariPool-1 - Added connection` — `Database version: 8.0.46` |
| Appel de l'endpoint | `Invoke-WebRequest http://localhost:8080/api/health` | `HTTP 200` et `{"status":"UP",...,"database":"UP",...}` |
| En-tête CORS | requête avec `Origin: http://localhost:4200` | `Access-Control-Allow-Origin: http://localhost:4200` |

### 3.6.6 Analyse critique

| Aspect | Évaluation |
|--------|-----------|
| Utile | Génération rapide d'une structure cohérente et commentée |
| Utile | Anticipation du conflit entre Spring Security et le health-check |
| Utile | Conception de tests indépendants de la base de données |
| Limite | Reproduction d'une pratique obsolète (déclaration explicite du dialecte Hibernate) |
| Intervention humaine | Suppression de la propriété superflue ; exigence d'une vérification par exécution réelle |

**Captures à réaliser :** `health_endpoint_postman.png`,
`backend_demarrage_console.png`, `backend_tests_succes.png`,
`hibernate_warning_avant_apres.png` (voir chapitre 6).

## 3.7 Tâche 4 — Génération du frontend Angular

### 3.7.1 Objectif

Initialiser le projet Angular, créer la page d'accueil Promptly avec sa
navigation, et établir la communication HTTP avec le backend.

### 3.7.2 Résultat obtenu

L'agent a généré le projet au moyen de la commande suivante, puis créé les
composants manuellement :

```powershell
ng new frontend --routing --style=css --ssr=false --skip-git --zoneless=false --package-manager=npm --ai-config=cursor --defaults
ng generate environments
```

Composants et services créés :

| Élément | Rôle |
|---------|------|
| `core/models/health.model.ts` | Interface TypeScript correspondant au `record` Java |
| `core/services/health.service.ts` | Appel de `GET /api/health` |
| `shared/components/navbar/` | Barre de navigation, liens des modules à venir désactivés |
| `shared/components/footer/` | Pied de page |
| `features/home/` | Page d'accueil, affichage de l'état du backend |

### 3.7.3 Problème constaté

Le fichier de test `app.spec.ts` généré automatiquement par Angular CLI
contenait une assertion portant sur le gabarit par défaut :

```typescript
expect(compiled.querySelector('h1')?.textContent).toContain('Hello, frontend');
```

Le gabarit par défaut ayant été remplacé par la mise en page de Promptly, ce test
était devenu caduc. Par ailleurs, le composant racine utilisant désormais le
routeur et `HttpClient`, l'instanciation aurait échoué faute de fournisseurs.

### 3.7.4 Correction appliquée

Le fichier de test a été réécrit avec les fournisseurs nécessaires
(`provideRouter`, `provideHttpClient`, `provideHttpClientTesting`) et des
assertions conformes au gabarit réel :

```typescript
expect(compiled.querySelector('app-navbar')).toBeTruthy();
expect(compiled.querySelector('app-footer')).toBeTruthy();
expect(compiled.querySelector('.brand__name')?.textContent).toContain('Promptly');
```

Le titre de la page dans `index.html` a également été corrigé (`Frontend` →
`Promptly`) et la langue du document fixée à `fr`.

### 3.7.5 Tests effectués

| Test | Commande | Résultat observé |
|------|----------|------------------|
| Compilation de production | `npm run build` | Succès en 7,2 s ; total initial 295,02 ko ; chargement différé de la page d'accueil effectif (`home` en *lazy chunk*) |
| Tests unitaires | `npm test -- --watch=false --browsers=ChromeHeadless` | `Executed 2 of 2 SUCCESS` |
| Analyse statique | diagnostics de l'éditeur | Aucune erreur |
| Serveur de développement | `npm start` | `Local: http://localhost:4200/` |
| Communication avec l'API | affichage dans le navigateur | `API : UP`, `BASE DE DONNEES : UP`, `Promptly 0.0.1-SNAPSHOT` |

### 3.7.6 Analyse critique

| Aspect | Évaluation |
|--------|-----------|
| Utile | Adoption spontanée des conventions d'Angular 20 (signaux, syntaxe `@if`/`@for`, nommage sans suffixe `.component`) |
| Utile | Centralisation de l'URL de l'API dans les fichiers d'environnement plutôt qu'en dur dans les composants |
| Limite | Le test généré par Angular CLI, laissé inchangé, aurait fait échouer la suite de tests ; sa correction a nécessité une vérification humaine |
| Intervention humaine | Exigence d'exécution effective des tests, qui a mis en évidence la nécessité d'ajouter les fournisseurs |

**Captures à réaliser :** `frontend_page_accueil.png`,
`frontend_build_succes.png`, `frontend_tests_succes.png`,
`frontend_appel_api_devtools.png` (voir chapitre 6).

## 3.8 Tâche 5 — Sécurisation des identifiants de base de données

### 3.8.1 Objectif

Configurer l'accès à MySQL sans inscrire le mot de passe dans le dépôt Git.

### 3.8.2 Contexte

Le mot de passe de l'instance MySQL locale a été communiqué à l'agent en réponse
à une question de configuration, et l'adresse d'un dépôt GitHub public a été
fournie pour l'hébergement du projet.

### 3.8.3 Résultat obtenu

L'agent n'a pas inscrit la valeur dans les fichiers de configuration. Il a
appliqué une lecture par variables d'environnement :

```properties
spring.datasource.username=${PROMPTLY_DB_USER:root}
spring.datasource.password=${PROMPTLY_DB_PASSWORD:}
```

Les variables ont été définies au niveau de l'utilisateur Windows, et
`.gitignore` a été complété pour exclure `application-local.properties`, `.env`
et les fichiers de clés. L'agent a par ailleurs signalé qu'un secret inscrit
dans un *commit* reste accessible dans l'historique Git même après suppression.

### 3.8.4 Analyse critique

| Aspect | Évaluation |
|--------|-----------|
| Utile | Application de la règle de non-codage en dur sans rappel explicite |
| Utile | Signalement spontané du risque lié à l'historique Git |
| Limite | Le mot de passe demeure présent dans l'historique de la conversation ; seul le dépôt est protégé |

**Capture à réaliser :** `securite_variables_environnement.png` (voir chapitre 6).

## 3.9 Synthèse de la phase 0

### 3.9.1 Bilan quantitatif

| Indicateur | Valeur |
|------------|--------|
| Fichiers de code rédigés par l'IA | 22 |
| Fichiers générés par les outils (Maven, Angular CLI) | 25 environ |
| Corrections humaines nécessaires | 3 (version de Spring Boot, dialecte Hibernate, test Angular) |
| Erreurs bloquantes détectées avant exécution | 1 (incompatibilité Node.js/Angular) |
| Tests automatisés écrits | 4 (2 backend, 2 frontend) |
| Tests en échec | 0 |

### 3.9.2 Apports constatés de l'IA

- **Diagnostic de l'environnement** : détection automatisée de l'incompatibilité
  Node.js 24 / Angular 18 avant toute écriture de code.
- **Rapidité de mise en place** : production d'une fondation complète et
  commentée en une session de travail.
- **Cohérence transversale** : nommage, organisation des paquetages et style de
  commentaires homogènes entre le backend et le frontend.
- **Explicitation des choix** : chaque décision technique a été accompagnée
  d'une justification, ce qui a directement alimenté le présent rapport.

### 3.9.3 Limites constatées

- **Connaissance figée** : la version de Spring Boot initialement recommandée ne
  correspondait plus à l'état réel du dépôt officiel.
- **Reproduction de pratiques obsolètes** : déclaration explicite du dialecte
  Hibernate, devenue superflue depuis Hibernate 6.
- **Absence de vérification spontanée** : les fichiers générés par les outils
  tiers (test par défaut d'Angular CLI) n'ont pas été contrôlés sans demande
  explicite.
- **Nécessité d'un cadrage strict** : sans la contrainte de progression par
  phases imposée dans le prompt, le volume de code produit aurait dépassé ce que
  l'étudiant peut maîtriser et expliquer.

### 3.9.4 Conclusion

L'IA s'est révélée efficace pour la **production** de code de fondation et pour
le **diagnostic** de l'environnement, mais insuffisante pour la **validation**.
Les trois corrections nécessaires n'ont été identifiées que par exécution réelle
(tests, journaux de démarrage, appels HTTP) ou par interrogation de sources
externes (Maven Central).

La règle de travail retenue pour les phases suivantes est donc la suivante :
*aucune affirmation de bon fonctionnement n'est acceptée sans trace d'exécution
vérifiable.*
