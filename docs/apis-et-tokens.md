# APIs externes et emplacement des tokens — Promptly

Document de reference : toutes les APIs / services utilises par le projet, et **ou coller les cles**.

## Ou mettre les secrets (regle generale)

| Emplacement | Role |
|-------------|------|
| `backend/src/main/resources/application-local.properties` | **Fichier principal pour les tokens** (gitignore — ne jamais committer) |
| Variables d'environnement Windows | Alternative / demarrage Maven (`PROMPTLY_*`) |
| `backend/src/main/resources/application.properties` | Config publique uniquement (URLs, flags) — **pas de secrets** |
| `frontend/src/environments/environment.development.ts` | Client ID Google (public cote navigateur) |

Importer local : `spring.config.import=optional:classpath:application-local.properties`

Modele minimal de `application-local.properties` :

```properties
# Hugging Face — emotion reclamations
promptly.emotion.hf-token=

# Sightengine — filtre gros mots forum
promptly.profanity.api-user=
promptly.profanity.api-secret=

# TextGears — auto-correct forum
promptly.spellcheck.api-key=
promptly.spellcheck.provider=textgears
promptly.spellcheck.language=fr-FR
```

---

## 1. TextGears — auto-correction (forum)

| | |
|--|--|
| **Usage** | Corrige orthographe / grammaire des posts et commentaires a la publication |
| **Endpoint** | `POST https://api.textgears.com/grammar` |
| **Compte** | https://textgears.com/signup.php (option *API and demo account*) |
| **Java** | `SpellCorrectService` |
| **Appelle depuis** | `ForumService.cleanUserText` |

**Tokens / config :**

```properties
# application-local.properties
promptly.spellcheck.enabled=true
promptly.spellcheck.provider=textgears
promptly.spellcheck.language=fr-FR   # ou en-US
promptly.spellcheck.api-key=VOTRE_CLE_TEXTGEARS
```

| Propriete | Env (optionnel) |
|-----------|-----------------|
| `promptly.spellcheck.api-key` | `PROMPTLY_SPELLCHECK_API_KEY` |

Option alternative : Azure Bing Spell Check (`provider=bing`, meme `api-key`).

---

## 2. Sightengine — filtre de gros mots (forum)

| | |
|--|--|
| **Usage** | Remplace les insultes par `***` dans posts / commentaires |
| **Endpoint** | `POST https://api.sightengine.com/1.0/text/check.json` |
| **Compte** | https://dashboard.sightengine.com/signup → API credentials |
| **Java** | `ProfanityFilterService` |
| **Appelle depuis** | `ForumService.cleanUserText` (apres TextGears) |

**Tokens / config :**

```properties
# application-local.properties
promptly.profanity.enabled=true
promptly.profanity.api-user=VOTRE_API_USER
promptly.profanity.api-secret=VOTRE_API_SECRET
```

Sans cles : fallback lexique local FR/EN uniquement.

---

## 3bis. Google Gemini — matching CV / offre (recrutement)

| | |
|--|--|
| **Usage** | Score competences + experience CV vs offre |
| **Endpoint** | `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent` |
| **Compte** | https://aistudio.google.com/apikey (gratuit) |
| **Java** | `CvMatchService` |

```properties
# application-local.properties
promptly.cv-match.enabled=true
promptly.cv-match.api-key=VOTRE_CLE_GEMINI
```

Sans cle : matching lexical (mots-cles) en secours. HF n'est **pas** utilise pour le recrutement.

---

## 3. Hugging Face Inference — emotion (reclamations)

| | |
|--|--|
| **Usage** | Detecte l'emotion d'une reclamation (admin) |
| **Endpoint** | `POST https://api-inference.huggingface.co/models/{model}` |
| **Modele** | `j-hartmann/emotion-english-distilroberta-base` |
| **Compte** | https://huggingface.co/settings/tokens |
| **Java** | `ComplaintEmotionService` |

**Tokens / config :**

```properties
# application-local.properties
promptly.emotion.enabled=true
promptly.emotion.hf-token=hf_XXXXXXXX
# optionnel :
# promptly.emotion.hf-model=j-hartmann/emotion-english-distilroberta-base
```

Sans token : fallback lexique local.

> Ne pas definir `promptly.emotion.hf-token=` vide dans `application.properties` (ecraserait le fichier local).

---

## 4. Google Sign-In — authentification

| | |
|--|--|
| **Usage** | Connexion / inscription via Google |
| **API** | Verification du Google ID token (bibliotheque Google) |
| **Compte** | Google Cloud Console → OAuth Client ID (type *Application Web*) |
| **Java** | `GoogleTokenService`, `AuthService` (`POST /api/auth/google`) |
| **Angular** | `environment.googleClientId` |

**Tokens / config :**

Backend (variable d'env recommandee au demarrage) :

```text
PROMPTLY_GOOGLE_CLIENT_ID=xxxxx.apps.googleusercontent.com
```

ou dans `application-local.properties` :

```properties
promptly.google.client-id=xxxxx.apps.googleusercontent.com
```

Frontend :

```ts
// frontend/src/environments/environment.development.ts
googleClientId: 'xxxxx.apps.googleusercontent.com'
```

Meme valeur cote backend et frontend. Le Client ID est **public** (navigateur) ; ce n'est pas un secret serveur.

---

## 5. Ollama — chatbot + resume / flashcards / quiz IA (local)

| | |
|--|--|
| **Usage** | Assistant IA (chat), resume PDF, flashcards, generation de quiz |
| **Endpoints** | `POST http://localhost:11434/api/chat` (assistant) ; `/api/generate` (resume / quiz) |
| **API Promptly** | `POST /api/ai/chat` |
| **Token** | Aucun (service local) |
| **Java** | `OllamaClient`, `AiChatService` |
| **UI** | `/ia` — Assistant IA |

**Config** (`application.properties`) :

```properties
promptly.ollama.base-url=http://localhost:11434
promptly.ollama.model=llama3.2:3b
```

Prealable : `ollama serve` et modele telecharge (`ollama pull llama3.2:3b`).

---

## 6. ml-integrity — GUESS / KNOW (local)

| | |
|--|--|
| **Usage** | Predire si une reponse quiz est au hasard (GUESS) ou connue (KNOW) |
| **Endpoint** | `POST http://localhost:8090/predict` |
| **Token** | Aucun |
| **Java** | `IntegrityMlClient` |
| **Service** | dossier `ml-integrity/` |

```properties
promptly.integrity.enabled=true
promptly.integrity.base-url=http://localhost:8090
```

---

## 7. ml-complaints — type / urgence / priorite (local)

| | |
|--|--|
| **Usage** | Classifier une reclamation (labels admin) |
| **Endpoint** | `POST http://localhost:8091/classify` |
| **Token** | Aucun |
| **Java** | `ComplaintMlClient` |
| **Service** | dossier `ml-complaints/` |

```properties
promptly.complaints-ml.enabled=true
promptly.complaints-ml.base-url=http://localhost:8091
```

---

## Autres secrets (pas des APIs tierces)

| Cle | Ou | Usage |
|-----|-----|--------|
| `PROMPTLY_JWT_SECRET` | Variable d'environnement | Signature des JWT (min. 32 caracteres) |
| `PROMPTLY_DB_PASSWORD` | Variable d'environnement | Mot de passe MySQL |
| `PROMPTLY_ADMIN_EMAIL` / `PROMPTLY_ADMIN_PASSWORD` | Variable d'environnement | Compte admin initial au 1er demarrage |

Exemple PowerShell avant `mvnw spring-boot:run` :

```powershell
$env:PROMPTLY_JWT_SECRET = 'PromptlyDevJwtSecretKeyAtLeast32Chars!'
$env:PROMPTLY_DB_PASSWORD = '000000'
$env:PROMPTLY_GOOGLE_CLIENT_ID = 'xxxxx.apps.googleusercontent.com'
$env:PROMPTLY_ADMIN_EMAIL = 'admin@promptly.local'
$env:PROMPTLY_ADMIN_PASSWORD = 'Admin2026!'
```

---

## Tableau recapitulatif

| Service | Token requis ? | Ou l'ajouter | Endpoint |
|---------|----------------|--------------|----------|
| **TextGears** | Oui (`api-key`) | `application-local.properties` | `https://api.textgears.com/grammar` |
| **Sightengine** | Oui (`api-user` + `api-secret`) | `application-local.properties` | `https://api.sightengine.com/1.0/text/check.json` |
| **Hugging Face** | Oui (`hf-token`) | `application-local.properties` | emotion reclamations |
| **Google Gemini** | Oui (`api-key`) | `application-local.properties` | matching CV / offre (recrutement) |
| **Google OAuth** | Client ID | env + `environment.development.ts` | verification ID token |
| **Ollama** | Non | — | `/api/chat` + `/api/generate` (local) |
| **ml-integrity** | Non | — | `http://localhost:8090/predict` |
| **ml-complaints** | Non | — | `http://localhost:8091/classify` |

---

## Flux forum (moderation)

```
Texte post / commentaire
        │
        ▼
  TextGears (auto-correct)     ← promptly.spellcheck.api-key
        │
        ▼
  Sightengine (*** gros mots) ← promptly.profanity.api-user / api-secret
        │
        ▼
  Enregistrement en base
```
