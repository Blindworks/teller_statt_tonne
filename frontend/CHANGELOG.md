# Changelog — Frontend

Alle nennenswerten Änderungen am Frontend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [0.5.3] - 2026-05-03

### Changed

- Pickups-Wochenwähler: Datums-Input (Startdatum) entfernt; KW-Label nun auch auf Mobile sichtbar. Navigation erfolgt über Pfeile und „Heute"-Button.

## [0.5.2] - 2026-05-03

### Changed

- Partner-Detail-Dialog: „Bearbeiten"-Button im Footer wird für Retter ausgeblendet. Admin/Botschafter sehen den Button unverändert.

## [0.5.1] - 2026-05-03

### Changed

- Stores-Übersicht: Edit-Button (Stift-Icon) auf den Partner-Kacheln wird für Retter ausgeblendet. Admin/Botschafter sehen den Button unverändert.

## [0.5.0] - 2026-05-03

### Changed

- Dashboard-Sidebar-Kachel „Slots heute" wurde rollenabhängig umgebaut: Retter sehen jetzt den nächsten freien Slot in einem ihrer zugeordneten Stores (Datum, Uhrzeit, Partnername, Anzahl freier Plätze als Badge); Klick auf die Kachel öffnet den Pickup. Ohne freien Slot in den nächsten 7 Tagen erscheint ein Empty State. Admin/Botschafter sehen die Anzahl der Slots heute mit mindestens einem freien Platz. NEW_MEMBER sehen die Kachel nicht.

## [0.4.0] - 2026-05-03

### Changed

- Dashboard: Die Kachel „Active Members" (statisch „42 / +12%") wurde durch eine Kachel „Nächster Pickup" ersetzt. Sie zeigt für die eingeloggte Person den nächsten eigenen Pickup mit Countdown (`Td Hh Mm`, Update alle 30 s), Datum, Uhrzeit und Partnername. Klick auf die Kachel öffnet den Pickup. Ohne anstehenden Pickup erscheint ein Empty State.

## [0.3.0] - 2026-05-03

### Added

- Schulungstext zum Aufnahme-Quiz wird im Startscreen als aufklappbares Akkordeon angezeigt (8 thematische Abschnitte: Motivation, Verantwortung, Zusammenarbeit mit Betrieben, Vorbereitung & Hygiene, Weitergabe, Zuverlässigkeit, Grenzfälle, Rolle des Vereins).
- Mobile Bottom-Navigation: Alle Menüeinträge der Desktop-Sidebar sind auf Mobile erreichbar. Die Bottom-Bar zeigt 4 primäre Tabs (Übersicht, Stores, Karte, Planer bzw. Benutzer je nach Rolle) plus einen **Mehr**-Button, der ein Bottom-Sheet mit den restlichen Einträgen (Benutzer/Planer Gegenstück, Store Zuweisungen, Papierkorb für Admins, Quiz, Profil, Abmelden) öffnet.

## [0.2.6] - 2026-05-03

### Changed

- Pickups (Rolle Retter): Buttons **Eintragen** / **Austragen** sind für Pickups in der Vergangenheit (Datum vor heute) ausgeblendet, vergangene Slots werden in der Wochenansicht abgedunkelt. Backend-Aufrufe für vergangene Pickups werden mit HTTP 410 abgewiesen und in der UI als Hinweis angezeigt.

## [0.2.5] - 2026-05-03

### Fixed

- Reload mit abgelaufenem Access-Token, aber noch gültigem Refresh-Token führt nicht mehr zum Logout: Der `authGuard` lässt geschützte Routen jetzt auch durch, wenn nur ein Refresh-Token vorhanden ist — der HTTP-Interceptor holt den neuen Access-Token beim ersten Request transparent nach.

## [0.2.4] - 2026-05-03

### Changed

- Pickups: Hero-Bento (Weekly Harvest Impact / Nächste Abholung) vorübergehend ausgeblendet. Die zugehörigen Signals/Methoden in der Komponente bleiben erhalten.

## [0.2.3] - 2026-05-03

### Fixed

- Im Pickups-Wochenkalender lassen sich jetzt auch sonntags Abholungen anlegen. Der bisherige "Sonntagsruhe"-Sonderzweig blockierte das Hinzufügen fälschlich; Sonntag verhält sich nun wie jeder andere Wochentag.

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
