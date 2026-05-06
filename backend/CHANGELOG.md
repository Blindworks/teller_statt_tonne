# Changelog — Backend

Alle nennenswerten Änderungen am Backend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

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
