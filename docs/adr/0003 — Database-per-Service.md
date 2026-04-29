# ADR 0003 — Database-per-Service

**Date :** 2026-04-28
**Statut :** Accepté

## Contexte

Le monolithe d'origine utilise **une seule base PostgreSQL partagée** par tous les domaines (`auth`, `menu`, `order`, `payment`, `user`). Les entités sont liées entre elles via des `@ManyToOne` / `@OneToMany` JPA directs (ex : `Order.user`, `Payment.order`).

Cette approche présente des problèmes critiques pour une architecture microservices :

- **Couplage de schéma** : modifier la table `users` (ex : renommer une colonne) casse tous les services qui la lisent.
- **Couplage transactionnel** : toutes les transactions touchent la même DB → impossible de scaler indépendamment.
- **Impossible de choisir le bon stockage par cas d'usage** (ex : Postgres pour Ordering, MongoDB pour Catalog).
- **Point unique de défaillance** : si la DB tombe, tous les services tombent.

## Décision

Chaque microservice **possède sa propre base PostgreSQL dédiée** :

- `identity-service` → `identity_db` (sa propre instance Postgres)
- `catalog-service` → `catalog_db`
- `ordering-service` → `ordering_db`
- `payment-service` → `payment_db`
- `delivery-service` → `delivery_db`
- `notification-service` → `notification_db`
- `reservation-service` → `reservation_db`
- `platform-admin-service` → `platform_admin_db`

**Règle stricte** : aucun service n'accède directement à la base d'un autre service. Toute donnée d'un autre domaine est récupérée via :
- **Appel REST synchrone** (si la donnée est nécessaire immédiatement).
- **Réplication via événement asynchrone** (CQRS pattern, Sprint 5+).

Les anciennes relations JPA inter-domaines (ex : `Order.user`) sont remplacées par des **références par ID** (ex : `Order.userId: UUID`).

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| **Base partagée (statu quo monolithique)** | ❌ Rejetée | Annule tous les bénéfices microservices (couplage, indisponibilité corrélée) |
| **Une base Postgres avec un schéma par service** | ❌ Rejetée | Mieux qu'une base unique, mais reste un SPOF + couplage opérationnel (mêmes upgrades, même backup) |
| **Une instance Postgres par service** | ✅ **Retenue** | Vraie autonomie — chaque service choisit sa version, ses backups, son scaling |

## Conséquences

### Positives
- **Autonomie de schéma** : chaque équipe (ou ici, chaque service) évolue à son rythme.
- **Résilience** : la panne d'une base n'affecte qu'un service.
- **Liberté de choix technique** : on peut migrer un service vers MongoDB/Redis sans toucher aux autres.
- **Scalabilité indépendante** : chaque DB est dimensionnée selon sa charge réelle.

### Négatives
- **Pas de transactions ACID inter-services** → on doit utiliser le pattern **Saga** pour les opérations multi-services (Sprint 5).
- **Pas de JOIN inter-services** → on duplique parfois des données (CQRS) ou on fait des appels REST.
- **Cohérence éventuelle** (eventual consistency) à gérer dans l'UX (badges "en cours de traitement"…).
- **Plus de complexité opérationnelle** (N bases à monitorer, backuper, upgrader).

## Implémentation locale

`docker-compose.yml` orchestrera **N conteneurs Postgres**, un par service, sur des ports différents :
- `identity_db` → port 5433
- `catalog_db` → port 5434
- `ordering_db` → port 5435
- etc.

En production, ces bases seraient hébergées sur des instances managées séparées (Railway, Supabase, AWS RDS…).

## Références

- Chris Richardson, *Microservices Patterns*, Manning 2018, Chap. 2 ("Database per Service").
- Sam Newman, *Building Microservices*, 2nd ed., Chap. 4 ("Splitting the Monolith").
- Martin Kleppmann, *Designing Data-Intensive Applications*, O'Reilly 2017.