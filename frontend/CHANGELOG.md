# Changelog — Frontend

Alle nennenswerten Änderungen am Frontend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

### Added

- Schulungstext zum Aufnahme-Quiz wird im Startscreen als aufklappbares Akkordeon angezeigt (8 thematische Abschnitte: Motivation, Verantwortung, Zusammenarbeit mit Betrieben, Vorbereitung & Hygiene, Weitergabe, Zuverlässigkeit, Grenzfälle, Rolle des Vereins).

## [0.2.2] - 2026-05-03

### Fixed

- Parallele 401-Antworten lösen nicht mehr mehrere konkurrierende `/api/auth/refresh`-Calls aus: `AuthService.refresh()` dedupliziert in-flight-Refreshes via `shareReplay`, sodass das Backend keinen DB-Konflikt auf `refresh_token` mehr produziert.
- Refresh-Fehler werden im Interceptor jetzt sauber unterschieden: 401/403 → Tokens leeren + Redirect zum Login; 5xx/Netzwerkfehler werden propagiert ohne erzwungenen Logout.

## [0.2.1] - 2026-05-03

### Fixed

- Abgelaufene Sessions führen jetzt zuverlässig zum Login: Der HTTP-Interceptor leitet auch dann zum `/login` weiter, wenn beim 401 kein Refresh-Token (mehr) vorhanden ist, und räumt Stale-Tokens auf.
- `authGuard` lässt geschützte Routen nur noch passieren, wenn das Access-JWT noch nicht abgelaufen ist (Prüfung des `exp`-Claims), statt nur das Vorhandensein eines Token-Strings zu prüfen.
- Netzwerkfehler (z. B. während eines Backend-Neustarts) setzen den Auth-State nicht mehr auf „ausgeloggt"; `currentUser` wird nur noch bei echten 401/403 zurückgesetzt.

## [0.2.0] - 2026-05-02

### Added

- Eigene Verfügbarkeit (wöchentliche Zeitfenster) kann jetzt direkt im Profil unter "Wann ich Zeit habe" gepflegt werden.

## [0.1.0] - 2026-05-01

Erste interne Version. Bündelt die bisherige Frontend-Entwicklung in einer SemVer-konformen Ausgangsbasis.

### Added

- Angular-21-App mit Standalone-Components, Vitest als Test-Runner.
- Routing und App-Konfiguration (`app.config.ts`, `app.routes.ts`).
- Kartenansicht auf Basis von Leaflet inkl. MarkerCluster und Custom-Location-Picker.
- Partner- und Store-Verwaltung über `partners`-Service.
- Pickup-/Slot-Flows mit rollenbasiertem Dashboard (Admin/Botschafter vs. Retter).
- Tailwind-CSS-Setup über `@tailwindcss/postcss`.
- `APP_VERSION`-Konstante in `src/app/version.ts`, gespeist aus `package.json`.
