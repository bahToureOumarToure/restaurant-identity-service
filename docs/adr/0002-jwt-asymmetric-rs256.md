# ADR 0002 — JWT signé en RS256 (asymétrique)

**Date :** 2026-04-28
**Statut :** Accepté

## Contexte

Dans une architecture microservices, plusieurs services doivent **valider** un JWT émis par `identity-service`. Deux stratégies cryptographiques sont possibles :

- **HS256 (symétrique)** : un secret unique partagé entre tous les services. Le même secret signe et vérifie.
- **RS256 (asymétrique)** : `identity-service` détient une **clé privée** pour signer. Tous les autres services possèdent la **clé publique** correspondante pour vérifier.

Le monolithe d'origine utilise HS256 avec un secret hardcodé dans `application.yml`.

## Décision

Nous adoptons **RS256 (RSA 2048 bits minimum)** :

- `identity-service` possède la clé privée (`identity-service/keys/private_key.pem`, gitignorée).
- `identity-service` expose un endpoint **JWKS** (`/api/v1/auth/.well-known/jwks.json`) qui distribue la clé publique au format standard.
- Les autres services récupèrent la clé publique au démarrage (ou la cachent localement) et **valident les tokens en local, sans appel réseau**.

## Alternatives considérées

| Option | Verdict | Raison |
|---|---|---|
| **HS256 secret partagé** | ❌ Rejetée | Couplage fort (changer le secret = redéployer tous les services). Compromission d'un service = compromission de tous. |
| **HS256 + appel à identity-service à chaque validation** | ❌ Rejetée | Latence + couplage temporel (identity en panne = tout en panne) + SPOF. |
| **RS256 + JWKS endpoint** | ✅ **Retenue** | Standard industrie (OAuth2, OIDC), validation locale, découplage cryptographique. |

## Conséquences

### Positives
- **Découplage** : chaque service valide les tokens sans dépendre d'identity-service au runtime.
- **Sécurité** : la clé privée ne quitte jamais identity-service. Compromettre un autre service ne permet pas de signer de faux tokens.
- **Performance** : validation locale = quelques microsecondes vs appel réseau (millisecondes).
- **Conformité standards** : compatible OAuth2/OIDC, prêt à être branché à un IdP externe (Keycloak, Auth0) si besoin futur.

### Négatives
- Plus complexe à mettre en place qu'un secret partagé.
- Nécessite de gérer la rotation des clés (out of scope pour le PFA — single-key suffit).
- Les tokens sont plus longs (signature ~256 bytes vs ~32 pour HS256).

## Implémentation

```
identity-service/keys/
├── private_key.pem      (PKCS#8, gitignoré, RESTE DANS identity-service)
└── public_key.pem       (distribuée via /jwks.json)
```

Les autres services configurent dans leur `application.yml` :
```yaml
jwt:
  jwks-uri: http://identity-service:8081/api/v1/auth/.well-known/jwks.json
```

Spring Security (via `oauth2-resource-server`) récupère et cache automatiquement la clé.

## Références

- RFC 7519 (JWT) — https://www.rfc-editor.org/rfc/rfc7519
- RFC 7517 (JWKS) — https://www.rfc-editor.org/rfc/rfc7517
- Spring Security OAuth2 Resource Server — https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
