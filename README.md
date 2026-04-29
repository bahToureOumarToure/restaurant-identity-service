# Restaurant Italien — Architecture Microservices

Migration d'un monolithe Spring Boot (`../backend/`) vers une architecture microservices distribuée.

> Ce projet est le résultat d'une refonte architecturale du monolithe d'origine, en suivant les principes du Domain-Driven Design (Bounded Contexts) et les bonnes pratiques de Sam Newman ("Building Microservices", 2nd edition).

## Architecture cible

8 services métier découpés par **bounded context** :

| Service | Responsabilité | Sprint d'extraction |
|---|---|---|
| `identity-service` | Auth, comptes, JWT, rôles (CLIENT/STAFF/ADMIN/SUPER_ADMIN) | S2 (validé) |
| `catalog-service` | Menu, catégories, disponibilité, prix | S3 |
| `ordering-service` | Panier, commande, statut, historique | S4 |
| `payment-service` | Stripe, transactions, factures | S5 |
| `delivery-service` | Mode livraison, coursier, ETA, GPS | S6 |
| `notification-service` | Emails (Mailgun) + Push (FCM) | S6 |
| `reservation-service` | Réservation tables, créneaux | S7 |
| `platform-admin-service` | Feature flags, mode maintenance, audit | S7 |

**Composants techniques :**
- `api-gateway/` — Spring Cloud Gateway + Realtime fan-out WebSocket (S3+)
- `service-registry/` — Eureka (S4)
- `docs/adr/` — Architectural Decision Records

## Stack technique

- **Java 17+ / Spring Boot 3.x** (cohérence avec le monolithe d'origine)
- **PostgreSQL 16** — une instance par service (Database-per-Service)
- **Flyway** — migrations versionnées (remplace `ddl-auto=update`)
- **JWT RS256 asymétrique** — signature centralisée par `identity-service`, validation locale partout
- **RabbitMQ** — communication événementielle (S5+)
- **Testcontainers** — tests d'intégration sur vrai Postgres éphémère
- **Docker / Docker Compose** — orchestration locale

## Démarrage

```bash
# Démarrer tous les services en local
docker compose up -d

# Démarrer un service en particulier
cd identity-service && mvn spring-boot:run

# Lancer les tests d'un service
cd identity-service && mvn test
```

## Structure du repo

```
restaurant-microservices/
├── identity-service/        Service d'authentification (Sprint 2)
├── docs/
│   └── adr/                 Architectural Decision Records
├── docker-compose.yml       Orchestration locale (à venir)
└── README.md
```

## Documentation

- Choix d'architecture détaillés : [`docs/adr/`](docs/adr/)
- Plan complet de migration : voir le plan PFA dans `~/.claude/plans/`

## Migration depuis le monolithe

Le code du monolithe d'origine se trouve dans `../backend/` et sert de **spécification exécutable** : il documente les règles métier validées que nous portons proprement dans les nouveaux services. Aucune dépendance technique entre les deux.
