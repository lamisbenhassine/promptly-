# Promptly — ML Integrite des quiz

Predicit si une reponse etudiante est un **GUESS** (au hasard) ou **KNOW** (connaissance).

## Installation (Windows)

```powershell
cd ml-integrity
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python train_model.py
uvicorn app:app --host 0.0.0.0 --port 8090
```

Le backend Spring appelle `http://localhost:8090/predict`.
Si le service est arrete, Spring utilise un heuristique de secours.
