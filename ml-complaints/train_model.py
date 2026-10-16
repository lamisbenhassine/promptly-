"""
Entrainement multi-tache pour les reclamations (FR + EN).
Urgence et type dependent du contenu objet + description.
"""

from __future__ import annotations

import random
from pathlib import Path

import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model.joblib"

TYPES = ["TECHNIQUE", "DISPONIBILITE", "PEDAGOGIQUE", "COMPTE", "AUTRE"]
PRIORITIES = ["HAUTE", "MOYENNE", "BASSE"]

# (texte, type, urgence, priorite) — exemples realistes FR/EN
SEED_EXAMPLES: list[tuple[str, str, str, str]] = [
    # TECHNIQUE + URGENT
    ("i can't open quiz\ni can't open quiz then the deadline after 5min i will get zero so can u fix this pls",
     "TECHNIQUE", "URGENT", "HAUTE"),
    ("can't open the quiz\ndeadline in 10 minutes please fix asap",
     "TECHNIQUE", "URGENT", "HAUTE"),
    ("quiz bug\nthe submit button does not work and exam starts now",
     "TECHNIQUE", "URGENT", "HAUTE"),
    ("Impossible d'ouvrir le quiz\nEcheance dans 5 min, je vais avoir zero, aidez-moi",
     "TECHNIQUE", "URGENT", "HAUTE"),
    ("PDF crash\nLe PDF du chapitre plante, examen dans une heure",
     "TECHNIQUE", "URGENT", "HAUTE"),
    ("erreur 500 quiz\nLe quiz renvoie une erreur serveur critique",
     "TECHNIQUE", "URGENT", "HAUTE"),
    # TECHNIQUE + NON_URGENT
    ("small quiz display bug\nWhen you can, the choice buttons look slightly misaligned",
     "TECHNIQUE", "NON_URGENT", "BASSE"),
    ("Suggestion technique\nPetite remarque sur le design du bouton sans urgence",
     "TECHNIQUE", "NON_URGENT", "BASSE"),
    ("flashcards layout\nNot urgent, flashcards wrap oddly on mobile",
     "TECHNIQUE", "NON_URGENT", "MOYENNE"),
    ("Bug mineur PDF\nQuand vous pourrez, zoom un peu lent",
     "TECHNIQUE", "NON_URGENT", "BASSE"),
    # DISPONIBILITE
    ("platform down\nServer offline since this morning before my exam",
     "DISPONIBILITE", "URGENT", "HAUTE"),
    ("site hors ligne\nPlateforme indisponible, examen dans 15 min",
     "DISPONIBILITE", "URGENT", "HAUTE"),
    ("timeout loading\nPage never loads, service unavailable",
     "DISPONIBILITE", "URGENT", "MOYENNE"),
    ("maintenance longue\nSans urgence, ressources absentes hier soir",
     "DISPONIBILITE", "NON_URGENT", "MOYENNE"),
    # PEDAGOGIQUE
    ("confusing chapter\nThe explanation of recursion is unclear, when you have time",
     "PEDAGOGIQUE", "NON_URGENT", "BASSE"),
    ("quiz content mismatch\nQuestions do not match the course, suggestion to review",
     "PEDAGOGIQUE", "NON_URGENT", "MOYENNE"),
    ("besoin de clarification\nNotion JWT mal expliquee, pour information",
     "PEDAGOGIQUE", "NON_URGENT", "BASSE"),
    ("contenu trop difficile\nExemples pratiques manquants dans le chapitre",
     "PEDAGOGIQUE", "NON_URGENT", "MOYENNE"),
    # COMPTE
    ("can't login\nPassword reset fails and I have an exam today",
     "COMPTE", "URGENT", "HAUTE"),
    ("compte bloque\nImpossible de me connecter avant l'examen",
     "COMPTE", "URGENT", "HAUTE"),
    ("change email\nWhen you can, I would like to update my account email",
     "COMPTE", "NON_URGENT", "BASSE"),
    ("profil role\nPetite remarque : mon role affiche etudiant alors que je suis enseignant",
     "COMPTE", "NON_URGENT", "MOYENNE"),
    # AUTRE
    ("UI suggestion\nLow priority idea for the homepage layout",
     "AUTRE", "NON_URGENT", "BASSE"),
    ("question generale\nPour information, comment fonctionne Promptly ?",
     "AUTRE", "NON_URGENT", "BASSE"),
    ("feedback\nNot urgent, overall experience is good",
     "AUTRE", "NON_URGENT", "BASSE"),
]

TEMPLATES: dict[str, list[str]] = {
    "TECHNIQUE": [
        "quiz will not open",
        "can't open the course PDF",
        "submit button broken",
        "javascript error in console",
        "API returns error {code}",
        "flashcards crash after generate",
        "le quiz plante avec erreur {code}",
        "impossible d'ouvrir le PDF",
        "bouton soumission ne marche pas",
    ],
    "DISPONIBILITE": [
        "server is down",
        "platform offline",
        "service unavailable timeout",
        "site ne repond plus",
        "serveur hors ligne",
        "ressources inaccessibles",
    ],
    "PEDAGOGIQUE": [
        "chapter explanation is confusing",
        "need more examples on {topic}",
        "quiz questions mismatch course",
        "contenu confus sur {topic}",
        "flashcards trop vagues",
        "demande de clarification pedagogique",
    ],
    "COMPTE": [
        "cannot reset password",
        "google login fails on my account",
        "mot de passe oublie",
        "compte bloque apres tentatives",
        "session expires in a loop",
    ],
    "AUTRE": [
        "general feedback about UI",
        "suggestion interface",
        "question administrative",
        "remarque diverse",
    ],
}

URGENT_SUFFIXES = [
    "deadline after 5min I will get zero, please fix",
    "exam in 10 minutes, urgent",
    "bloquant pour mon examen maintenant",
    "critique, aide rapidement",
    "asap please fix this",
    "dans 5 min je perds tous les points",
]

CALM_SUFFIXES = [
    "no rush, when you can",
    "sans urgence, a votre convenance",
    "suggestion only, not urgent",
    "petite remarque pour information",
    "low priority whenever possible",
]


def synthesize(n: int = 3200, seed: int = 42):
    rng = random.Random(seed)
    texts: list[str] = []
    types: list[str] = []
    urgencies: list[str] = []
    priorities: list[str] = []

    for subject_desc, ctype, urg, prio in SEED_EXAMPLES:
        # Repeter les exemples cles pour bien les ancrer.
        for _ in range(12):
            texts.append(subject_desc)
            types.append(ctype)
            urgencies.append(urg)
            priorities.append(prio)

    for _ in range(n):
        ctype = rng.choice(TYPES)
        base = rng.choice(TEMPLATES[ctype]).format(
            code=rng.choice(["500", "404", "403"]),
            topic=rng.choice(["JWT", "SQL", "recursion", "Spring"]),
        )
        # Probabilite d'urgence selon le type (pas tout urgent).
        urgent_rate = {
            "TECHNIQUE": 0.40,
            "DISPONIBILITE": 0.55,
            "COMPTE": 0.35,
            "PEDAGOGIQUE": 0.12,
            "AUTRE": 0.08,
        }[ctype]
        urgent = rng.random() < urgent_rate
        if urgent:
            text = f"{base}\n{rng.choice(URGENT_SUFFIXES)}"
            urgency = "URGENT"
            priority = rng.choices(PRIORITIES, weights=[0.70, 0.25, 0.05])[0]
        else:
            text = f"{base}\n{rng.choice(CALM_SUFFIXES)}"
            urgency = "NON_URGENT"
            priority = rng.choices(PRIORITIES, weights=[0.05, 0.35, 0.60])[0]

        if urgency == "URGENT" and priority == "BASSE":
            priority = "MOYENNE"
        if urgency == "NON_URGENT" and priority == "HAUTE":
            priority = "MOYENNE"

        texts.append(text)
        types.append(ctype)
        urgencies.append(urgency)
        priorities.append(priority)

    return texts, types, urgencies, priorities


def fit_pipeline(texts: list[str], labels: list[str]) -> Pipeline:
    pipeline = Pipeline(
        steps=[
            (
                "tfidf",
                TfidfVectorizer(
                    ngram_range=(1, 2),
                    min_df=1,
                    max_features=6000,
                    lowercase=True,
                ),
            ),
            (
                "clf",
                LogisticRegression(
                    max_iter=1200,
                    class_weight="balanced",
                    random_state=42,
                ),
            ),
        ]
    )
    pipeline.fit(texts, labels)
    return pipeline


def main() -> None:
    texts, types, urgencies, priorities = synthesize()
    X_train, X_test, y_type_train, y_type_test = train_test_split(
        texts, types, test_size=0.2, random_state=42, stratify=types
    )
    _, _, y_urg_train, y_urg_test = train_test_split(
        texts, urgencies, test_size=0.2, random_state=42, stratify=types
    )
    _, _, y_prio_train, y_prio_test = train_test_split(
        texts, priorities, test_size=0.2, random_state=42, stratify=types
    )

    type_model = fit_pipeline(X_train, y_type_train)
    urgency_model = fit_pipeline(X_train, y_urg_train)
    priority_model = fit_pipeline(X_train, y_prio_train)

    print(f"Accuracy type     : {type_model.score(X_test, y_type_test):.3f}")
    print(f"Accuracy urgence  : {urgency_model.score(X_test, y_urg_test):.3f}")
    print(f"Accuracy priorite : {priority_model.score(X_test, y_prio_test):.3f}")

    # Smoke test sur le cas utilisateur.
    sample = "i can't open quiz\ni can't open quiz then the deadline after 5min i will get zero so can u fix this pls"
    print("Sample type    :", type_model.predict([sample])[0])
    print("Sample urgency :", urgency_model.predict([sample])[0])
    print("Sample priority:", priority_model.predict([sample])[0])

    joblib.dump(
        {
            "type_model": type_model,
            "urgency_model": urgency_model,
            "priority_model": priority_model,
            "types": TYPES,
            "urgencies": ["URGENT", "NON_URGENT"],
            "priorities": PRIORITIES,
        },
        MODEL_PATH,
    )
    print(f"Modele enregistre: {MODEL_PATH}")


if __name__ == "__main__":
    main()
