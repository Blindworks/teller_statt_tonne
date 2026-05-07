# Changelog — Backend

Alle nennenswerten Änderungen am Backend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [0.10.1] - 2026-05-07

### Fixed

- `AuthorizationDeniedException: Access Denied` auf Async-Dispatches (insbesondere SSE `/api/notifications/stream`): Der `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) lief per Default nicht auf `ASYNC`/`ERROR`-Dispatches, sodass beim erneuten Durchlauf der Security-Filter-Chain im Stateless-Modus (kein `SecurityContextRepository`) der `SecurityContextHolder` leer war und `AuthorizationFilter` (läuft in Spring Security 7 auf allen Dispatch-Typen) den Re-Dispatch ablehnte. Filter überschreibt jetzt `shouldNotFilterAsyncDispatch()` und `shouldNotFilterErrorDispatch()` mit `false`, parst das Bearer-Token erneut aus dem mitgesendeten `Authorization`-Header und befüllt den Context.

## [0.10.0] - 2026-05-07

### Added

- Auto-Notiz bei Status-Änderung eines Betriebs: `PartnerService.update`, `delete` und `restore` legen automatisch eine interne Notiz (`Visibility.INTERNAL`) im Notiz-Verlauf des Betriebs an, sobald sich `Partner.Status` ändert. Body-Format: `Status geändert: <alt> → <neu>` mit deutschsprachigen Status-Labels. Author ist der ausführende User (`CurrentUser.requireId()`).

## [0.9.0] - 2026-05-07

### Added

- Erwartete Menge in Kilogramm pro Betriebs-Slot: `PartnerEntity.PickupSlotEmbeddable` und DTO `Partner.PickupSlot` haben das neue Feld `expectedKg` (BigDecimal, nullable). Liquibase-Changeset 014 fügt Spalte `expected_kg` zu `partner_pickup_slot` hinzu.
- Beim Anlegen eines Pickups (`POST /api/pickups`) wird `savedKg` jetzt im DTO `Pickup` exposed und – falls nicht explizit gesetzt – aus dem zum Termin (Wochentag, Start-/Endzeit) passenden Slot übernommen.
- Globaler Statistik-Endpoint `GET /api/stats/overview` (Rollen ADMINISTRATOR/TEAMLEITER): liefert Gesamt-`savedKg`, Anzahl abgeschlossener Pickups, Top-10-Betriebe und Top-10-Retter (jeweils Summe `savedKg` und Pickup-Anzahl).

## [0.8.0] - 2026-05-07

### Added

- Bewerbungen auf Betriebe: Retter (auch `NEW_MEMBER`) können sich über `POST /api/partner-applications` auf einen Betrieb bewerben; Admin/Teamleitung genehmigen via `POST /api/partner-applications/{id}/approve` (fügt User in `partner_user` ein) oder lehnen mit Begründung ab via `POST /api/partner-applications/{id}/reject`. Retter ziehen eigene `PENDING`-Bewerbungen via `DELETE /api/partner-applications/{id}` zurück. Listen unter `GET /api/partners/{partnerId}/applications` (Admin/Teamleitung) und `GET /api/users/me/partner-applications` (Eigene). Neue Entity `PartnerApplicationEntity` mit Status `PENDING`/`APPROVED`/`REJECTED`/`WITHDRAWN`; Liquibase-Changeset 013 legt Tabelle `partner_application` an. SSE-Notifications: neuer Bewerbungs-Eingang an Admin/Teamleitung, Entscheidung an Retter (`PARTNER_APPLICATION_RECEIVED`/`APPROVED`/`REJECTED`).

### Changed

- Rolle `BOTSCHAFTER` umbenannt zu `TEAMLEITER` (Label "Teamleitung"). Liquibase-Changeset 012 aktualisiert die Datenbank-Zeile in `role`. Alle `@PreAuthorize`-Strings, Tests und `hasRole`-Checks im Code wurden angepasst. Bestehende Migrationen (`007-roles.xml`) bleiben aus Historiengründen unverändert.

## [0.7.0] - 2026-05-07

### Added

- Notiz-Funktion für Betriebe (`PartnerNoteEntity`): Append-only Notizen pro Betrieb mit Sichtbarkeits-Flag (`INTERNAL` nur für Admin/Botschafter, `SHARED` zusätzlich für Retter) und Soft-Delete (nur Admin/Botschafter). Endpunkte unter `/api/partners/{partnerId}/notes` (GET/POST), Soft-Delete via `DELETE /{noteId}`. Liquibase-Changeset 011 legt Tabelle `partner_note` an.

## [0.6.1] - 2026-05-07

### Fixed

- Dashboard zeigte für Admin/Botschafter nicht verplante Tage als Template-Slots auf Basis wiederkehrender Partner-Slots an (z.B. Sonntag 10.05., obwohl im Abholung-Planer kein Pickup angelegt war). `DashboardService.findRangeSlots` liefert nun ausschließlich tatsächlich angelegte `Pickup`-Datensätze — Dashboard und Planer sind dadurch konsistent.

## [0.6.0] - 2026-05-07

### Changed

- **Breaking:** `Partner.Status` ersetzt das bisherige Lifecycle-Enum (`ACTIVE`/`INACTIVE`/`DELETED`) durch sieben Kooperationsstatus: `KEIN_KONTAKT` (Default für neu angelegte Betriebe), `VERHANDLUNGEN_LAUFEN`, `WILL_NICHT_KOOPERIEREN`, `KOOPERIERT`, `KOOPERIERT_FOODSHARING`, `SPENDET_AN_TAFEL`, `EXISTIERT_NICHT_MEHR`. `EXISTIERT_NICHT_MEHR` übernimmt die Soft-Delete-Rolle (Filterung in `findAll`/`findAllForMember`, `delete`/`restore`, `countMembersGroupedByPartner`). `restore` setzt einen wiederhergestellten Betrieb auf `KEIN_KONTAKT`. Liquibase-Changeset 010 erweitert `partner.status` auf `VARCHAR(32)` und mappt Bestandsdaten (`ACTIVE`→`KOOPERIERT`, `INACTIVE`→`VERHANDLUNGEN_LAUFEN`, `DELETED`→`EXISTIERT_NICHT_MEHR`).
- `DashboardService.getDaySlots` zeigt Slot-Templates nur noch für Betriebe mit Status `KOOPERIERT`.

### Added

- Pickup-Restriktion: `PickupService.create` lehnt das Anlegen von Pickups mit `IllegalArgumentException` ab, wenn der Betrieb nicht den Status `KOOPERIERT` hat. `PickupSignupService.signup` liefert den neuen `Result.PARTNER_NOT_COOPERATING` (HTTP `409 Conflict`) zurück, wenn ein Retter sich für einen Pickup eines nicht (mehr) kooperierenden Betriebs eintragen möchte.

## [0.5.0] - 2026-05-07

### Added

- Nachrichtensystem (Phase 1, System-Meldungen): neues `notification`-Paket mit `NotificationEntity`, `NotificationService`, `NotificationController` unter `/api/notifications` (`GET`, `GET /unread-count`, `GET /stream` als Server-Sent-Events, `PATCH /{id}/read`, `POST /read-all`). Meldungen entstehen automatisch via Spring `ApplicationEvent`s (`PickupUnassignedEvent`, `PickupStatusChangedEvent`); `NotificationEventListener` (`@TransactionalEventListener` AFTER_COMMIT) verteilt sie an alle Retter und Botschafter des betroffenen Partners ausser dem Auslöser. Echtzeit-Push pro User über `NotificationStreamService` (`SseEmitter`-Map mit 25-s-Heartbeat). Liquibase-Changeset 009 legt die `notification`-Tabelle inkl. FKs auf `app_user`/`pickup`/`partner` an.
- `@EnableScheduling` in `BackendApplication` für den SSE-Heartbeat.

## [0.4.2] - 2026-05-07

### Changed

- `DELETE /api/pickups/{id}/signup`: Retter können sich nur noch bis 2 Stunden vor `startTime` aus einem Pickup austragen. Innerhalb dieser Frist antwortet der Endpoint mit HTTP `422 Unprocessable Entity` (neuer `Result.UNASSIGN_TOO_LATE` im `PickupSignupService`). Eintragen (`POST`) bleibt unverändert.

## [0.4.1] - 2026-05-07

### Changed

- `UserController`: `POST`, `PUT` und `DELETE` auf `/api/users` sind jetzt durch `@PreAuthorize("hasAnyRole('ADMINISTRATOR','BOTSCHAFTER')")` geschützt. Bisher waren `PUT` und `DELETE` ohne Rollencheck — jeder authentifizierte Nutzer (auch Retter) konnte Stammdaten ändern oder löschen. `POST /{id}/photo` bleibt unverändert (Self-Upload + Admin via `isAuthorizedForUser`).

## [0.4.0] - 2026-05-06

### Added

- Rollen werden jetzt in der Datenbank verwaltet: neue Tabelle `role` (Liquibase-Changeset 007) plus Join-Tabelle `user_role`. Die vier bisherigen Rollen (`ADMINISTRATOR`, `BOTSCHAFTER`, `RETTER`, `NEW_MEMBER`) werden initial geseedet, bestehende `app_user.role`-Zuordnungen migriert, anschließend wird die Spalte gedroppt. Ein User kann mehrere Rollen tragen (`Set<RoleEntity>`).
- Neue REST-Endpoints unter `/api/roles` für Auflisten/Anlegen/Bearbeiten/Löschen von Rollen (Mutations `@PreAuthorize("hasRole('ADMINISTRATOR')")`). Lösch-/Deaktivierungsversuch der `ADMINISTRATOR`-Rolle wird mit HTTP 409 abgelehnt, da sie hartkodiert in `@PreAuthorize`-Ausdrücken referenziert ist.
- Liquibase-Changeset 008 als Sicherheitsnetz: stellt idempotent sicher, dass die bekannten Admin-Konten `admin@example.de` (Prod) und `admin@local` (Dev) nach der Rollen-Migration die `ADMINISTRATOR`-Rolle besitzen. Verhindert „kein Admin mehr im System"-Situationen, falls die 007-Migration einen Account ohne Alt-Rolle vorgefunden hat.

### Changed

- JWT-Access-Token enthält künftig den Claim `roles: string[]` statt `role: string`. Der `JwtAuthenticationFilter` liest die Liste und legt pro Eintrag eine `ROLE_<name>`-Authority an. Tokens mit altem `role`-Claim werden eine Release lang als Fallback weiter akzeptiert.
- `User`-DTO und `LoginResponse` liefern `roles: string[]` statt `role: string`. `AdminCreateUserRequest` erwartet jetzt `roleNames: Set<String>`.
- `GET /api/users/roles` liest die Optionen aus der DB statt aus dem Java-Enum (Antwort-Shape `{value, label}` bleibt).

### Fixed

- Apple Web Push funktioniert wieder: `PushSubscriptionService` rief `pushService.send(notification)` ohne Encoding-Argument auf — die Bibliothek fiel dadurch auf das veraltete `AESGCM`-Format (draft-04) zurück, das nur Chrome/Firefox akzeptieren. Apple lehnt das mit `403 BadAuthorizationHeader` ab und verlangt `AES128GCM` (RFC 8291). Versand erfolgt jetzt explizit mit `Encoding.AES128GCM`.

## [0.3.3] - 2026-05-06

### Added

- Neuer Endpoint `GET /api/holidays?from=YYYY-MM-DD&to=YYYY-MM-DD` liefert deutsche gesetzliche Feiertage (bundesweit + NRW-Spezifika: Fronleichnam, Allerheiligen). Bewegliche Feiertage werden über die Gauß'sche Osterformel berechnet, das Feld `region` (`"DE"` oder `"NRW"`) erlaubt eine spätere Erweiterung auf weitere Bundesländer.

## [0.3.2] - 2026-05-06

### Fixed

- Apple Web Push (`web.push.apple.com`) lehnte VAPID-JWTs mit `403 BadAuthorizationHeader` ab. Ursache war das von `nl.martijndwars:web-push:5.1.1` transitiv mitgelieferte `jose4j 0.7.0`, das ES256-Signaturen in einem von Apple nicht akzeptierten Format ausgibt. `jose4j` ist jetzt explizit auf `0.9.6` angehoben (transitiver Eintrag wird ausgeschlossen).

### Changed

- Web Push Diagnose: Beim Start loggt `WebPushConfig` ob VAPID-Keys konfiguriert sind (maskierter Public-Key, Subject) bzw. eine Warnung, falls Keys fehlen. Bei einem Push-Fehler ≥ 400 wird jetzt zusätzlich der Response-Body des Push-Services als Reason geloggt, damit Ursachen wie `BadJwtSignature`, `JwtTokenExpired` oder `BadSubject` direkt sichtbar sind. Subscriptions werden bei `403` bewusst nicht gelöscht (Server-Misskonfiguration soll nicht alle Geräte abmelden).

## [0.3.1] - 2026-05-04

### Added

- Neuer Endpoint `POST /api/push/test`, der eine Test-Push-Benachrichtigung an den aufrufenden User sendet.

## [0.3.0] - 2026-05-04

### Added

- Web Push (VAPID): Neue Endpoints unter `/api/push/*` zur Verwaltung von Push-Subscriptions (`GET /vapid-public-key` öffentlich, `POST/DELETE /subscriptions` authentifiziert). Subscriptions werden in Tabelle `push_subscription` (Liquibase-Changeset 006) persistiert und an `app_user` gekoppelt (Cascade-Delete). Versand erfolgt asynchron über `PushSubscriptionService` mit der Bibliothek `nl.martijndwars:web-push`; abgelaufene Subscriptions (HTTP 404/410) werden automatisch entfernt. VAPID-Schlüssel und Subject werden ausschließlich aus `application.properties` (`app.vapid.*`) bezogen.

## [0.2.2] - 2026-05-03

### Changed

- `POST/DELETE /api/pickups/{id}/signup`: Pickups deren Datum vor dem aktuellen Tag liegt können nicht mehr ein- oder ausgetragen werden. Beide Endpoints antworten in diesem Fall mit HTTP `410 Gone` (neuer `Result.PICKUP_PAST` im `PickupSignupService`).

## [0.2.1] - 2026-05-03

### Fixed

- Race-Condition beim parallelen `/api/auth/refresh`: Statt das alte Refresh-Token per `update ... set revoked=true` zu markieren (was unter Last zu MariaDB-Fehler „Record has changed since last read" führte), wird die Zeile jetzt atomar via `delete ... where id = ? and revoked = false` entfernt. Konkurrierende Refreshes erhalten sauber 401 statt 500.

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
