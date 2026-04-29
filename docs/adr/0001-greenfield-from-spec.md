# ADR 0001 — Stratégie de migration : Greenfield from Spec

**Date :** 2026-04-28
**Statut :** Accepté
**Décideurs :** Équipe PFA

## Contexte

Le projet a été initié comme un monolithe Spring Boot par un précédent contributeur (`../backend/`). L'analyse a révélé plusieurs anti-patterns structurels :

- Couplage JPA inter-domaines via `@ManyToOne` (Order → User, Payment → Order)
- Base PostgreSQL unique partagée par tous les domaines
- Secret JWT hardcodé dans `application.yml`
- Schéma DB géré par `spring.jpa.hibernate.ddl-auto=update` (anti-pattern en production)
- Aucune migration versionnée (pas de Flyway/Liquibase)

L'objectif du projet est de migrer vers une **architecture microservices** conforme aux principes de Sam Newman (*Building Microservices*, 2nd ed., 2021).

**Contrainte clé :** rien n'est en production. Aucun utilisateur à protéger, aucune donnée à migrer.

## Décision

Nous adoptons la stratégie **"Greenfield from Spec"** :

1. Création d'un **nouveau dossier `restaurant-microservices/`** structuré proprement, à côté du monolithe existant.
2. Le code monolithique `../backend/` reste **en lecture seule** comme **spécification exécutable** : il documente les règles métier validées (calculs prix, validations, règles JWT…).
3. Nous **portons la logique métier** dans les nouveaux services, **sans copier les anti-patterns** structurels.
4. Aucune coexistence en production des deux systèmes — pas de Strangler Fig à orchestrer.

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| **A — Strangler Fig in-place** | ❌ Rejetée | Pertinent pour un système en production. Sans utilisateurs, complexité injustifiée. |
| **B — Greenfield pur (oublier l'existant)** | ❌ Rejetée | Perte du travail métier validé (modèles, règles, validations). |
| **C — Greenfield from Spec** | ✅ **Retenue** | Combine clean architecture + réutilisation de la logique métier validée. |

## Conséquences

### Positives
- Conception clean dès le jour 1, sans compromis hérités.
- Réutilisation de la logique métier déjà validée dans le monolithe.
- Démonstration plus claire au jury (architecture cible directement opérationnelle, pas d'état transitoire).
- Permet de poser les bonnes fondations (RS256, Database-per-Service, Flyway, Testcontainers) dès le début.

### Négatives
- Demande de la discipline pour ne pas copier-coller sans réfléchir les anti-patterns.
- Le monolithe d'origine n'est pas livré (devient documentation, pas produit).

## Références

- Sam Newman, *Building Microservices*, 2nd edition, O'Reilly 2021.
- Martin Fowler, *Strangler Fig Application*, https://martinfowler.com/bliki/StranglerFigApplication.html
