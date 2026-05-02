# Changelog — Backend

Alle nennenswerten Änderungen am Backend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [0.2.0] - 2026-05-02

### Security

- `UserAvailabilityController` mit `@PreAuthorize` abgesichert: Nutzer dürfen nur die eigene Verfügbarkeit lesen/schreiben, Administrator und Botschafter dürfen alle.

## [0.1.0] - 2026-05-01

Erste interne Version. Bündelt die bisherige Backend-Entwicklung in einer SemVer-konformen Ausgangsbasis.

### Added

- Spring-Boot-4-Backend (Java 21) mit Web-MVC, Data JPA, Validation.
- Authentifizierung mit Spring Security und JWT (Login, Refresh, Logout).
- Liquibase-Migrationen für das Datenbankschema.
- MariaDB als Produktions-Datenbank, H2 für Tests.
- Mail-Versand über `spring-boot-starter-mail`.
- Domänen: Partner, Stores, Pickups (Slot-Eintragung durch Retter).
- CORS-Konfiguration über `app.cors.allowed-origins`.
- Versionsendpunkt `GET /api/version` (gespeist aus `BuildProperties`).
