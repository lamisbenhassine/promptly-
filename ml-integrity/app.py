"""
Service ML d'integrite des reponses quiz (GUESS vs KNOW).

Demarrage :
  cd ml-integrity
  python -m venv .venv
  .venv\\Scripts\\activate
  pip install -r requirements.txt
  python train_model.py
  uvicorn app:app --host 0.0.0.0 --port 8090
"""

from __future__ import annotations

from pathlib import Path
from typing import Literal

import joblib
import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model.joblib"

app = FastAPI(title="Promptly Quiz Integrity ML", version="1.0.0")


class FeatureItem(BaseModel):
    questionId: int
    timeSpentMs: int = Field(ge=0)
    selectionChanges: int = Field(ge=0)
    choiceIndex: int = Field(ge=0)
    isCorrect: bool
    avgTimeMs: float = Field(ge=0)


class PredictRequest(BaseModel):
    items: list[FeatureItem]


class Prediction(BaseModel):
    questionId: int
    label: Literal["GUESS", "KNOW"]
    confidence: float


class PredictResponse(BaseModel):
    predictions: list[Prediction]


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


@app.get("/health")
def health():
    ok = MODEL_PATH.exists()
    return {"status": "UP" if ok else "DOWN", "model": str(MODEL_PATH), "ready": ok}


@app.post("/predict", response_model=PredictResponse)
def predict(body: PredictRequest):
    bundle = load_model()
    pipeline = bundle["pipeline"]
    labels = bundle["labels"]

    if not body.items:
        return PredictResponse(predictions=[])

    matrix = np.array(
        [
            [
                item.timeSpentMs,
                item.selectionChanges,
                item.choiceIndex,
                1.0 if item.isCorrect else 0.0,
                item.avgTimeMs,
            ]
            for item in body.items
        ],
        dtype=float,
    )

    proba = pipeline.predict_proba(matrix)
    classes = list(pipeline.classes_)
    predictions: list[Prediction] = []

    for i, item in enumerate(body.items):
        # classe 1 = GUESS dans l'entrainement
        guess_idx = classes.index(1) if 1 in classes else 0
        know_idx = classes.index(0) if 0 in classes else (1 - guess_idx)
        p_guess = float(proba[i][guess_idx])
        p_know = float(proba[i][know_idx])
        if p_guess >= p_know:
            label = "GUESS"
            confidence = p_guess
        else:
            label = "KNOW"
            confidence = p_know
        # securite si mapping labels custom
        label = labels.get(1 if label == "GUESS" else 0, label)
        # Une mauvaise reponse ne peut pas etre "KNOW" (l'etudiant ne connaissait pas).
        if not item.isCorrect:
            label = "GUESS"
            confidence = max(p_guess, 0.75)
        predictions.append(
            Prediction(questionId=item.questionId, label=label, confidence=round(confidence, 4))
        )

    return PredictResponse(predictions=predictions)
