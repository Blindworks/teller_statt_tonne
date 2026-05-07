# Changelog — Frontend

Alle nennenswerten Änderungen am Frontend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [0.8.6] - 2026-05-07

### Fixed

- Build-Warnungen `NG8107` und `NG8102` in `assign-member-dialog.html` und `user-profile-dialog.component.html` beseitigt: überflüssige `?.`- und `??`-Operatoren auf `roles[0]` entfernt (`roles` ist als nicht-nullable `RoleName[]` typisiert). Verhalten unverändert.

## [0.8.5] - 2026-05-07

### Fixed

- Session-Verlust führt nun zuverlässig zum Login-Screen: `authGuard` akzeptiert kein bloßes Refresh-Token mehr und wartet bei vorhandenem Access-Token auf `me()`, bevor die Shell freigegeben wird. `AuthService.me()` verwirft bei 401/403 alle Tokens. `AppShellComponent` reagiert reaktiv auf Auth-Verlust (Effect → Redirect zu `/login`) und rendert ihre interne UI nur, solange der User authentifiziert ist — kein Aufblitzen von Sidebar/Topbar nach Logout oder abgelaufener Session.

## [0.8.4] - 2026-05-07

### Changed

- Sidebar und Mehr-Sheet: Menüpunkt „Store Zuweisungen" (`/admin/store-members`) ist nur noch für `ADMINISTRATOR` und `BOTSCHAFTER` sichtbar. Die Route ist durch `roleGuard(['ADMINISTRATOR','BOTSCHAFTER'])` geschützt; Retter werden bei Direktaufruf umgeleitet.
- Partnerdetails-Dialog: Schließen-Button hat jetzt feste 40×40 Box mit Flex-Zentrierung, sodass der Hover-Hintergrund tatsächlich rund ist.

## [0.8.3] - 2026-05-07

### Changed

- Partnerdetails-Dialog: Karten-Marker nutzt jetzt denselben Kategorie-Pin wie die Hauptkarte (statt Leaflet-Default-Marker). Marker-Styles sind nach `styles.css` gewandert und werden vom neuen Helper `map-marker-icon.ts` gebaut.

## [0.8.2] - 2026-05-07

### Changed

- Sidebar und Mehr-Sheet: Menüpunkt „Quiz" (`/admin/quiz/questions`) ist nur noch für `ADMINISTRATOR` und `BOTSCHAFTER` sichtbar. Die Routen `/admin/quiz/**` sind durch `roleGuard(['ADMINISTRATOR','BOTSCHAFTER'])` geschützt; Retter werden bei Direktaufruf umgeleitet.

## [0.8.1] - 2026-05-07

### Changed

- UsersView: Edit-Stift an Nutzerkarten und „Den Kreis erweitern"-Karte sind nur noch für `ADMINISTRATOR` und `BOTSCHAFTER` sichtbar. Routen `/users/new` und `/users/edit/:id` sind durch `roleGuard(['ADMINISTRATOR','BOTSCHAFTER'])` geschützt; Retter sehen die Liste weiterhin read-only.

## [0.8.0] - 2026-05-06

### Added

- Neuer Admin-Bereich `/admin` (lazy-loaded, geschützt durch `roleGuard(['ADMINISTRATOR'])`) mit Übersichts-Dashboard. Erstes Modul: Rollenverwaltung unter `/admin/roles` (CRUD-Tabelle mit Inline-Lösch-Confirm) und `/admin/roles/new` bzw. `/admin/roles/:id` für ein Reactive-Form. Eintrag „Administration" in Sidebar und Mobile-Sheet (Material-Icon `admin_panel_settings`).
- Service `RoleService` (`src/app/admin/roles/role.service.ts`) und Modell-Typen für die neuen `/api/roles`-Endpoints. `UserService` cached `GET /api/users/roles` und kann den Cache invalidieren, wenn der Admin Rollen anlegt/löscht.

### Changed

- User-Datenmodell: `User.role: Role` → `User.roles: string[]`. `Role` ist jetzt ein String-Alias (`RoleName`), Rollennamen sind dynamisch und nicht mehr hartkodierte Literal-Union. `roleGuard`, `isAdmin`/`isPlanner` und alle Komponenten, die Rollen prüfen, nutzen `roles.includes(...)`. Die Bestandsanzeige in Benutzerliste/-edit zeigt vorerst die erste Rolle des Users (`roles[0]`); Mehrfach-Zuweisung über UI ist Folge-Arbeit.
- `AdminCreateUserRequest` sendet jetzt `roleNames: string[]` statt `role: string` (passend zum Backend).

## [0.7.1] - 2026-05-06

### Added

- Pickups-Wochenansicht: gesetzliche Feiertage (bundesweit + NRW: Fronleichnam, Allerheiligen) werden im Tageskopf als Label angezeigt und der Tagesblock farblich hervorgehoben. Daten kommen aus dem neuen Backend-Endpoint `GET /api/holidays`.
- Pickup-Erstellung/-Bearbeitung: Hinweisbox erscheint, wenn das gewählte Datum auf einen Feiertag fällt (Hinweis auf möglicherweise geschlossenen Store).

## [0.7.0] - 2026-05-06

### Added

- Öffentliche Rechtsseiten `/impressum` und `/datenschutz` (Lazy-Loaded Standalone-Components unter `src/app/legal/`). Vorlagen gemäß § 5 DDG / § 18 MStV bzw. Art. 13 DSGVO mit klar markierten Platzhaltern (`{{…}}`) für vereinsspezifische Angaben (Anschrift, Vorstand, Vereinsregister, Hoster). Footer-Links auf der Landing-Seite verweisen jetzt auf diese Routen.

## [0.6.1] - 2026-05-04

### Added

- Profil: Knopf „Test-Push senden" im Push-Benachrichtigungen-Block, der über `POST /api/push/test` eine Test-Notification an den aktuellen User schickt.

## [0.6.0] - 2026-05-04

### Added

- PWA-Unterstützung: Angular-Service-Worker (`@angular/service-worker`) wird in Production registriert, neues `manifest.webmanifest` und `ngsw-config.json` erlauben Installation auf Android/iOS-Homescreen.
- Web Push (VAPID): Neuer `PushNotificationService` (`src/app/push/`) kapselt Abonnement, Abbestellen und Permission-Status; integriert mit `SwPush.notificationClicks` für Deep-Links. Im Profil gibt es jetzt einen Block „Push-Benachrichtigungen" zum Aktivieren/Deaktivieren. VAPID Public Key wird aus `environment.vapidPublicKey` (Fallback: `GET /api/push/vapid-public-key`) bezogen, Subscriptions werden via `POST/DELETE /api/push/subscriptions` synchronisiert.

## [0.5.4] - 2026-05-03

### Fixed

- Pickup-Kachel (Wochenansicht): „Eingetragen"-Badge sprengte bei Retter-Sicht die Kachelbreite. Header-Row darf jetzt umbrechen (`flex-wrap` + `gap-1`), sodass der Badge bei schmalen Spalten unter den Kategorie-Chip rutscht statt überzulaufen.

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
