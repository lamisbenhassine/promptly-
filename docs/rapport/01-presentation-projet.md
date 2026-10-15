# 1. Présentation du projet

## 1.1 Contexte

Le présent rapport décrit la conception et la réalisation de **Promptly**, une
plateforme web d'apprentissage en ligne (*e-learning*) consacrée à
l'enseignement de la programmation, du développement logiciel et de
l'algorithmique.

Le projet s'inscrit dans un cadre académique dont l'une des exigences
principales est de **documenter l'utilisation de l'intelligence artificielle
durant le cycle de développement**. Cette exigence est traitée selon deux axes
rigoureusement distincts :

1. **L'IA comme assistant de développement** — utilisation d'outils d'IA
   génératifs (Cursor) pour concevoir, générer, corriger, expliquer et tester le
   code. Cet axe est documenté au chapitre 3.
2. **L'IA intégrée au produit** — fonctionnalité du logiciel final reposant sur
   un modèle de langage (local via Ollama, ou distant via une API externe). Cet
   axe est documenté au chapitre 4.

Cette distinction est essentielle : dans le premier cas l'IA est un *outil de
production*, dans le second elle est un *composant du livrable*.

## 1.2 Objectifs

L'objectif général est de fournir une application web permettant à des étudiants
de suivre des cours de programmation, d'évaluer leurs acquis et d'échanger entre
eux.

Les objectifs spécifiques sont les suivants :

- proposer un catalogue de cours organisé par technologie (Java, Spring Boot,
  Angular, TypeScript, algorithmique et structures de données) ;
- structurer chaque cours en chapitres contenant du contenu pédagogique, des
  exemples de code et des ressources ;
- évaluer les connaissances au moyen de questionnaires à choix multiples avec
  calcul automatique des scores ;
- animer une communauté d'apprenants grâce à un forum de discussion ;
- assurer le suivi des réclamations déposées par les étudiants ;
- intégrer une fonctionnalité d'assistance basée sur un modèle de langage.

## 1.3 Acteurs du système

Les acteurs identifiés à ce stade de l'analyse sont les suivants. Leurs droits
précis seront définis lors de la réalisation du module d'authentification.

| Acteur | Description | Principales interactions prévues |
|--------|-------------|----------------------------------|
| Visiteur | Utilisateur non authentifié | Consulter la page d'accueil, s'inscrire |
| Étudiant | Apprenant inscrit | Suivre les cours, passer les quiz, participer au forum, déposer une réclamation |
| Enseignant | Auteur de contenu pédagogique | Créer et modifier cours, chapitres et quiz |
| Administrateur | Responsable de la plateforme | Gérer les utilisateurs, les rôles et les réclamations |

## 1.4 Modules fonctionnels

L'application est découpée en sept modules, développés **de manière
incrémentale** : un module est entièrement conçu, réalisé, testé et documenté
avant que le suivant ne soit abordé.

| N° | Module | Description synthétique | État |
|----|--------|-------------------------|------|
| 0 | Template et fondation | Architecture technique, communication Angular/Spring Boot, page d'accueil | **Réalisé** |
| 1 | Authentification et utilisateurs | Inscription, connexion, profils, rôles, restrictions d'accès | **Réalisé** |
| 2 | Gestion des cours | CRUD des cours, catégories, affectation aux enseignants | **Réalisé** |
| 3 | Gestion des chapitres | Découpage des cours, contenus et exemples de code | Planifié |
| 4 | Évaluations et quiz | QCM, soumission des réponses, calcul des scores | Planifié |
| 5 | Forum et communauté | Publications, commentaires, modération | Planifié |
| 6 | Réclamations | Dépôt, traitement et suivi du statut des réclamations | Planifié |
| 7 | Intégration de l'IA | Fonctionnalité d'assistance par modèle de langage | Planifié |

> **Remarque méthodologique.** Conformément aux règles de rédaction retenues, ce
> rapport distingue systématiquement les fonctionnalités **réalisées et
> vérifiées** des fonctionnalités **planifiées**. Aucun résultat non observé
> n'est rapporté.

## 1.5 Périmètre de la phase 0

La phase 0 ne contient **aucune logique métier**. Elle a pour seul objet
d'établir une fondation technique vérifiée. Son périmètre est le suivant :

**Inclus :**

- initialisation du projet Spring Boot (Maven, Java 17) ;
- initialisation du projet Angular (TypeScript, routage) ;
- configuration de la connexion à la base de données MySQL ;
- endpoint de vérification `GET /api/health` ;
- configuration CORS autorisant le dialogue entre les deux applications ;
- page d'accueil présentant le nom Promptly et une barre de navigation ;
- structure documentaire du rapport et des captures d'écran ;
- vérification effective du démarrage des deux applications.

**Exclus :**

- toute entité métier (utilisateur, cours, chapitre, quiz, etc.) ;
- Spring Security et la gestion de l'authentification ;
- toute fonctionnalité d'IA intégrée à l'application.

## 1.6 Organisation du rapport

| Chapitre | Contenu |
|----------|---------|
| 1 | Présentation du projet (ce chapitre) |
| 2 | Architecture et conception technique |
| 3 | Utilisation de l'IA comme assistant de développement |
| 4 | Intégration de l'IA dans l'application |
| 5 | Tests et validation |
| 6 | Index des captures d'écran |
| Annexe | Journal chronologique des prompts (`journal-prompts.md`) |
