"""
Entrainement d'un classifieur GUESS vs KNOW (scikit-learn).
Genere des donnees synthetiques realistes puis sauvegarde model.joblib.
"""

from __future__ import annotations

import joblib
import numpy as np
from pathlib import Path
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model.joblib"


def synthesize(n: int = 4000, seed: int = 42) -> tuple[np.ndarray, np.ndarray]:
    rng = np.random.default_rng(seed)
    # features: time_spent_ms, selection_changes, choice_index, is_correct, avg_time_ms
    X = np.zeros((n, 5), dtype=float)
    y = np.zeros(n, dtype=int)

    for i in range(n):
        avg = float(rng.uniform(8000, 45000))
        if rng.random() < 0.45:
            # GUESS: tres rapide, peu de reflexion, parfois beaucoup de changements
            time_ms = float(rng.uniform(400, 5000))
            changes = int(rng.integers(0, 5))
            correct = int(rng.random() < 0.28)
            label = 1  # GUESS
        else:
            # KNOW: temps raisonnable autour de la moyenne
            time_ms = float(rng.normal(avg, avg * 0.25))
            time_ms = max(5000.0, time_ms)
            changes = int(rng.integers(0, 2))
            correct = int(rng.random() < 0.78)
            label = 0  # KNOW

        choice_index = int(rng.integers(0, 4))
        X[i] = [time_ms, changes, choice_index, correct, avg]
        y[i] = label

    return X, y


def main() -> None:
    X, y = synthesize()
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    pipeline = Pipeline(
        steps=[
            ("scaler", StandardScaler()),
            (
                "clf",
                RandomForestClassifier(
                    n_estimators=120,
                    max_depth=8,
                    random_state=42,
                    class_weight="balanced",
                ),
            ),
        ]
    )
    pipeline.fit(X_train, y_train)
    acc = pipeline.score(X_test, y_test)
    print(f"Accuracy hold-out: {acc:.3f}")

    joblib.dump({"pipeline": pipeline, "labels": {0: "KNOW", 1: "GUESS"}}, MODEL_PATH)
    print(f"Modele enregistre: {MODEL_PATH}")


if __name__ == "__main__":
    main()
