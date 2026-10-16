# Promptly — ML Reclamations

Classe automatiquement une reclamation selon :

| Champ | Valeurs |
|-------|---------|
| **Type** | TECHNIQUE, DISPONIBILITE, PEDAGOGIQUE, COMPTE, AUTRE |
| **Urgence** | URGENT, NON_URGENT |
| **Priorite** | HAUTE, MOYENNE, BASSE |

L'**emotion** (colere, content, etc.) est geree dans le **backend Spring**
via Hugging Face — pas dans ce micro-service.

## Installation (Windows)

```powershell
cd ml-complaints
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python train_model.py
uvicorn app:app --host 0.0.0.0 --port 8091
```

Le backend Spring appelle `http://localhost:8091/classify`.
Si le service est arrete, Spring utilise un heuristique de secours (mots-cles).

L'utilisateur depose seulement objet + description.
Seul l'**admin** voit type, urgence et priorite.
