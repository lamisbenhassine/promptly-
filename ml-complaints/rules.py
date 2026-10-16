"""
Regles lexicales (FR + EN) appliquees apres le modele.
Ne forcent URGENT que si des signaux clairs apparaissent dans objet/description.
"""

from __future__ import annotations

import re

# Signaux forts d'urgence (deadline, echec imminent, blocage).
URGENT_PATTERNS = [
    r"\burgent\b",
    r"\basap\b",
    r"\bcritique\b",
    r"\bbloquant\b",
    r"\bimmediat(?:e|ement)?\b",
    r"\bimmediately\b",
    r"\bdeadline\b",
    r"\bech[eé]ance\b",
    r"\bafter\s+\d+\s*min",
    r"\bdans\s+\d+\s*min",
    r"\b\d+\s*min(?:ute)?s?\b",
    r"\bget\s+zero\b",
    r"\bwill\s+get\s+0\b",
    r"\baurai\s+z[eé]ro\b",
    r"\bnote\s+0\b",
    r"\bexamen\b",
    r"\bexam\b",
    r"\bcan'?t\s+open\b",
    r"\bcannot\s+open\b",
    r"\bimpossible\s+d[' ]ouvrir\b",
    r"\bne\s+(?:marche|fonctionne)\s+plus\b",
    r"\bbloqu[eé]\b",
    r"\bstuck\b",
    r"\baide\s+rapidement\b",
    r"\bfix\s+this\s+(?:pls|please)\b",
    r"\bplease\s+fix\b",
]

# Signaux explicites de non-urgence.
CALM_PATTERNS = [
    r"\bsans\s+urgence\b",
    r"\bno\s+rush\b",
    r"\bwhen\s+you\s+(?:can|have\s+time)\b",
    r"\bquand\s+vous\s+pourrez\b",
    r"\ba\s+votre\s+convenance\b",
    r"\bsuggestion\b",
    r"\bpetite\s+remarque\b",
    r"\bpour\s+information\b",
    r"\blow\s+priority\b",
    r"\bnot\s+urgent\b",
]

CATEGORY_RULES: list[tuple[str, list[str]]] = [
    (
        "TECHNIQUE",
        [
            r"\bbug\b",
            r"\berreur\b",
            r"\berror\b",
            r"\bcrash\b",
            r"\bquiz\b",
            r"\bpdf\b",
            r"\bcan'?t\s+open\b",
            r"\bcannot\s+open\b",
            r"\bimpossible\s+d[' ]ouvrir\b",
            r"\bdoesn'?t\s+work\b",
            r"\bne\s+(?:marche|fonctionne)\b",
            r"\bjavascript\b",
            r"\bapi\b",
            r"\b500\b",
            r"\b404\b",
        ],
    ),
    (
        "DISPONIBILITE",
        [
            r"\bhors\s+ligne\b",
            r"\boffline\b",
            r"\bindisponible\b",
            r"\bunavailable\b",
            r"\bdown\b",
            r"\btimeout\b",
            r"\bne\s+r[eé]pond\b",
            r"\bserver\b.*\bdown\b",
        ],
    ),
    (
        "COMPTE",
        [
            r"\bcompte\b",
            r"\baccount\b",
            r"\bpassword\b",
            r"\bmot\s+de\s+passe\b",
            r"\blogin\b",
            r"\bconnexion\b",
            r"\bprofil\b",
            r"\bsession\b",
        ],
    ),
    (
        "PEDAGOGIQUE",
        [
            r"\bpedagog\b",
            r"\bexplication\b",
            r"\bconfus\b",
            r"\bclarif",
            r"\bexamples?\b",
            r"\bexemples?\b",
            r"\bflashcards?\b",
            r"\bnotion\b",
            r"\bcontenu\b",
            r"\bdifficult[eé]\b",
        ],
    ),
]


def _matches_any(text: str, patterns: list[str]) -> bool:
    return any(re.search(p, text, flags=re.IGNORECASE) for p in patterns)


def refine(subject: str, description: str, ctype: str, urgency: str, priority: str) -> tuple[str, str, str]:
    text = f"{subject or ''}\n{description or ''}".lower()

    # Categorie : premiere regle qui matche (ordre = priorite metier).
    for label, patterns in CATEGORY_RULES:
        if _matches_any(text, patterns):
            ctype = label
            break

    calm = _matches_any(text, CALM_PATTERNS)
    urgent_signal = _matches_any(text, URGENT_PATTERNS)

    if calm and not urgent_signal:
        urgency = "NON_URGENT"
        if priority == "HAUTE":
            priority = "MOYENNE"
    elif urgent_signal:
        urgency = "URGENT"
        # Deadline / note 0 / examen => priorite haute.
        if _matches_any(
            text,
            [
                r"\bdeadline\b",
                r"\bexamen\b",
                r"\bexam\b",
                r"\bget\s+zero\b",
                r"\b\d+\s*min",
                r"\bcritique\b",
                r"\bbloquant\b",
            ],
        ):
            priority = "HAUTE"
        elif priority == "BASSE":
            priority = "MOYENNE"

    if urgency == "URGENT" and priority == "BASSE":
        priority = "MOYENNE"
    if urgency == "NON_URGENT" and priority == "HAUTE" and not urgent_signal:
        priority = "MOYENNE"

    return ctype, urgency, priority
