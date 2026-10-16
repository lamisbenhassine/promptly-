# Rapport LaTeX — IA & Promptly

## Compiler le PDF

Dans ce dossier (`docs/rapport-latex/`) :

```powershell
latexmk -pdf main.tex
```

Ou :

```powershell
pdflatex main.tex
pdflatex main.tex
```

Ouvrir `main.pdf`.

## Captures d'ecran (obligatoires)

| Dossier | Contenu |
|---------|---------|
| `figures/chatgpt/` | Prompt ChatGPT + reponse (prompt Cursor) |
| `figures/cursor/` | Prompts / resultats / corrections Cursor |
| `figures/app/` | Fonctionnalites IA dans l'appli |

Noms conseilles : voir `figures/README-captures.md`.

Apres avoir ajoute une image, dans le `.tex` correspondant :
1. Enlever le `\fbox{...}` placeholder
2. Decommenter `\includegraphics[...]{...}`

## Personnaliser la couverture

Dans `main.tex`, modifier `\author{...}` (nom, universite, annee).
