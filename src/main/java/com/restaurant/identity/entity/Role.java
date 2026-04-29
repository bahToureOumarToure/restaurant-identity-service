package com.restaurant.identity.entity;

/**
 * Rôles possibles dans le système.
 * IMPORTANT : ces valeurs DOIVENT correspondre exactement à la contrainte CHECK
 * de la migration Flyway V1__create_users_table.sql
 */
public enum Role {
    CLIENT,       // Utilisateur final (passe des commandes)
    STAFF,        // Personnel restaurant (cuisine, service)
    ADMIN,        // Administrateur métier (gère menu, commandes…)
    SUPER_ADMIN;  // Administrateur plateforme (maintenance services)

    /**
     * Convention Spring Security : préfixer les autorités par "ROLE_".
     */
    public String authority() {
        return "ROLE_" + name();
    }
}