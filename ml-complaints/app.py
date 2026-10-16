"""
Service ML de classement des reclamations.

Predictions :
  - type       : TECHNIQUE | DISPONIBILITE | PEDAGOGIQUE | COMPTE | AUTRE
  - urgency    : URGENT | NON_URGENT
  - priority   : HAUTE | MOYENNE | BASSE

L'emotion est geree cote backend Spring (API Hugging Face), pas ici.
"""

from __future__ import annotations

from pathlib import Path
from typing import Literal

import joblib
from fastapi import FastAPI
from pydantic import BaseModel, Field

from rules import refine

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model.joblib"

app = FastAPI(title="Promptly Complaints ML", version="1.3.0")

TypeLabel = Literal["TECHNIQUE", "DISPONIBILITE", "PEDAGOGIQUE", "COMPTE", "AUTRE"]
UrgencyLabel = Literal["URGENT", "NON_URGENT"]
PriorityLabel = Literal["HAUTE", "MOYENNE", "BASSE"]


class ClassifyRequest(BaseModel):
    subject: str = Field(min_length=1, max_length=300)
    description: str = Field(min_length=1, max_length=8000)


class ClassifyResponse(BaseModel):
    type: TypeLabel
    urgency: UrgencyLabel
    priority: PriorityLabel
    confidence: float


_bundle = None


def load_model():
    global _bundle
    if _bundle is None:
        if not MODEL_PATH.exists():
            raise RuntimeError(
                "model.joblib manquant. Lancez d'abord: python train_model.py"
            )
        _bundle = joblib.load(MODEL_PATH)
    return _bundle


def _best_label(pipeline, text: str) -> tuple[str, float]:
    proba = pipeline.predict_proba([text])[0]
    classes = list(pipeline.classes_)
    idx = int(proba.argmax())
    return str(classes[idx]), float(proba[idx])


@app.get("/health")
def health():
    ok = MODEL_PATH.exists()
    return {"status": "UP" if ok else "DOWN", "model": str(MODEL_PATH), "ready": ok}


@app.post("/classify", response_model=ClassifyResponse)
def classify(body: ClassifyRequest):
    bundle = load_model()
    subject = body.subject.strip()
    description = body.description.strip()
    text = f"{subject}\n{description}"

    ctype, c_type = _best_label(bundle["type_model"], text)
    urgency, c_urg = _best_label(bundle["urgency_model"], text)
    priority, c_prio = _best_label(bundle["priority_model"], text)

    ctype, urgency, priority = refine(subject, description, ctype, urgency, priority)

    confidence = round((c_type + c_urg + c_prio) / 3.0, 4)
    return ClassifyResponse(
        type=ctype,  # type: ignore[arg-type]
        urgency=urgency,  # type: ignore[arg-type]
        priority=priority,  # type: ignore[arg-type]
        confidence=confidence,
    )
