# Changelog — Backend

Alle nennenswerten Änderungen am Backend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [0.20.0] - 2026-05-11

### Added

- Veranstaltungen als kurzlebige Abholungsorte: neues Modul `event` mit Entity, DTO, Mapper, Repository, Service und Controller (`/api/events`). Felder: `name`, `description`, `startDate`/`endDate` (Pflicht), Adresse (Straße/PLZ/Stadt/Lat/Lng — Geocoding über `GeocodingService`), optionales `logoUrl` und ein eingebetteter `Contact` (Name/E-Mail/Telefon). `GET /api/events?scope=active|past|all` (Default `active`, filtert auf `endDate >= heute`/`< heute`). Anlegen, Bearbeiten, Löschen und Logo-Upload erfordern `ADMINISTRATOR` oder `TEAMLEITER`. Neue Systemlog-Eventtypen `EVENT_CREATED`, `EVENT_UPDATED`, `EVENT_DELETED` (Kategorie `ADMIN_ACTION`).
- Liquibase-Changeset 022 legt Tabelle `event` an (inkl. Index auf `end_date`), ergänzt `pickup.event_id` mit FK auf `event.id` (`ON DELETE CASCADE`), macht `pickup.partner_id` nullable und sichert per CHECK-Constraint `ck_pickup_parent_xor`, dass jedes Pickup genau einen Eltern-Datensatz (Betrieb ODER Veranstaltung) besitzt.

### Changed

- `PickupEntity` hat jetzt eine zweite optionale ManyToOne-Referenz `event` (Spalte `event_id`); `partner` ist optional. `Pickup`-DTO und `DaySlot`-DTO um `eventId`, `eventName`, `eventLogoUrl` erweitert.
- `PickupService` validiert beim Anlegen/Aktualisieren, dass genau eines von `partnerId`/`eventId` gesetzt ist. Sichtbarkeitsfilter: `RETTER` sieht alle Event-Pickups (offen, ohne Partner-Zuordnung) zusätzlich zu Pickups der Betriebe, denen er zugeordnet ist. `createSeries` ist nur für Betriebs-Pickups erlaubt; Veranstaltungs-Pickups werden manuell pro Slot angelegt.
- `PickupSignupService.signup` überspringt für Event-Pickups die Membership- und Partner-Status-Checks — jede:r RETTER kann sich in einen freien Event-Slot eintragen.
- `DashboardService` nimmt Event-Pickups in die DaySlot-Aggregation auf und blendet sie für RETTER nicht über die Partner-Mitgliedschaft aus.

### Added

- Neues Modul `partnercategory` mit Entity, DTO, Mapper, Repository, Service und Controller (`/api/partner-categories`). `GET` ist öffentlich lesbar (für Dropdowns/Filter im Frontend), `GET /all`, `POST`, `PUT`, `DELETE` erfordern `ADMINISTRATOR` oder `TEAMLEITER`. `DELETE` liefert `409 Conflict`, wenn die Kategorie noch von Betrieben verwendet wird; Kategorien können stattdessen über `active=false` weich deaktiviert werden.
- Liquibase-Changeset 021 legt Tabelle `partner_category` mit Spalten `id`, `code` (UNIQUE), `label`, `icon`, `order_index`, `active` an, seedet die fünf bestehenden Kategorien (BAKERY, SUPERMARKET, CAFE, RESTAURANT, BUTCHER) und migriert die `partner.category`-VARCHAR-Spalte auf einen `category_id`-FK.

### Changed

- `Partner` (DTO und Entity) verwendet statt eines `Category`-Enums jetzt `categoryId: Long` mit FK auf `partner_category.id`. Das innere Enum `Partner.Category` wurde entfernt.
- `Pickup.partnerCategory` und `DaySlot.partnerCategory` heißen jetzt `partnerCategoryId: Long` und führen den FK statt eines Enum-Werts.

## [0.18.0] - 2026-05-11

### Added

- Verteilerplätze (intern „Verteilerplatz", extern „Teller-Treff"): neues Modul `distributionpoint` mit Entity, DTO, Mapper, Repository, Service und Controller (`/api/distribution-points`). CRUD-Endpunkte sind auf `ADMINISTRATOR` und `TEAMLEITER` beschränkt. Felder: Name, Beschreibung, Adresse (Straße, PLZ, Stadt, optional Lat/Lng), Betreiber als `@ManyToMany` zu `UserEntity` (Join-Table `distribution_point_operator`) und Öffnungszeiten als `@ElementCollection` (`distribution_point_opening_slot`, Wochentag + Start-/Endzeit, mehrere Slots pro Wochentag möglich).
- Liquibase-Changeset 020 legt die Tabellen `distribution_point`, `distribution_point_operator` und `distribution_point_opening_slot` an (Cascade-Delete auf den Verteilerplatz).
- Neue Systemlog-Eventtypen `DISTRIBUTION_POINT_CREATED`, `DISTRIBUTION_POINT_UPDATED`, `DISTRIBUTION_POINT_DELETED` (Kategorie `ADMIN_ACTION`).

## [0.17.0] - 2026-05-11

### Added

- `Partner.Category` um den Wert `BUTCHER` (Metzgerei) erweitert.

## [0.16.0] - 2026-05-08

### Added

- Einladungs-Mail beim Anlegen eines Users: `UserService.adminCreate` versendet via `PasswordResetService.sendInvitation` automatisch eine Mail mit einem Link zum initialen Passwort-Setzen (TTL 30 Min, gleicher Mechanismus wie Password-Reset; eigener `MailKind.INVITATION` mit angepasstem Betreff/Body).
- Neuer Endpoint `POST /api/users/{id}/resend-invitation` (`ADMINISTRATOR`/`TEAMLEITER`): generiert neuen Token, invalidiert vorherige Einladungs-/Reset-Tokens des Nutzers und sendet die Einladungs-Mail erneut. Wirft `409 Conflict`, wenn der Nutzer bereits ein Passwort gesetzt hat.
- Neuer Systemlog-Eventtyp `USER_INVITATION_SENT`.
- `User`-DTO um `hasPassword: boolean` erweitert, damit das Frontend den Resend-Button korrekt anzeigen kann.

### Changed

- `AdminCreateUserRequest` enthält kein `password`-Feld mehr — der angelegte User vergibt sein Passwort selbst über den Einladungs-Link.
- `UserEntity.passwordHash` ist nullable (Liquibase 019 entfernt das `NOT NULL`-Constraint), damit ein User vor dem Setzen seines Initialpassworts existieren kann.
- `AuthService.login` lehnt Login mit `passwordHash == null` sauber als ungültige Anmeldedaten ab (kein NPE).

## [0.15.0] - 2026-05-08

### Added

- Erweiterter User-Lebenszyklus: `UserEntity.Status`-Enum auf `PENDING`, `ACTIVE`, `PAUSED`, `LEFT`, `REMOVED` umgestellt. Neue Spalte `app_user.introduction_completed_at` (Liquibase 018) erfasst, wann ein Admin/Teamleiter das Einführungsgespräch bestätigt hat. Liquibase migriert bestehendes `INACTIVE` nach `LEFT` und setzt den Default auf `PENDING`, sodass neu angelegte Konten das Onboarding explizit durchlaufen.
- Neue dedizierte Übergangs-Endpoints am `UserController` (alle `ADMINISTRATOR`/`TEAMLEITER`-only, `leave` zusätzlich für den Nutzer selbst): `POST /api/users/{id}/introduction-completed`, `/pause`, `/reactivate`, `/leave`, `/remove`. Jeder Übergang validiert den Quell-Status und antwortet bei unzulässigen Wechseln mit `409 Conflict`.
- `UserService.promoteToActiveIfReady` setzt einen Nutzer automatisch auf `ACTIVE`, sobald `introductionCompletedAt != null` und ein `APPROVED`-Hygienezertifikat vorliegt; aufgerufen aus `markIntroductionCompleted` und nach `HygieneCertificateService.approve`.
- `UserResponse`-DTO um `introductionCompletedAt` (Instant) und `hygieneApproved` (boolean, abgeleitet aus `HygieneCertificate`) erweitert, damit das Frontend den Onboarding-Fortschritt anzeigen kann.
- Neuer Systemlog-Eventtyp `USER_STATUS_CHANGED` für jeden Statuswechsel.

### Changed

- `PUT /api/users/{id}` ändert den `status` nicht mehr — Status wird ausschließlich über die dedizierten Übergangs-Endpoints verwaltet.

## [0.14.0] - 2026-05-08

### Added

- Systemlog für Administratoren: Neue Tabelle `system_log` (Liquibase 017) mit Spalten für `event_type`, `severity`, `category`, `actor_user_id`/`actor_email`, `target_type`/`target_id`, `message`, `details`, `ip_address`, `user_agent`. Indizes auf `created_at`, `event_type`, `(category, created_at)`, `actor_user_id`. FK auf `app_user` mit `ON DELETE SET NULL`, sodass Logs eine Nutzer-Löschung überleben.
- Neuer Endpoint `GET /api/admin/system-log` (nur `ADMINISTRATOR`, via `@PreAuthorize`) mit Pagination (`page`, `size`, default 50, max 200), Sortierung `createdAt DESC`, und Filtern: `category`, `eventType`, `severity`, `actorUserId`, `from`, `to` (ISO-Instants), `search` (Volltext über `message`/`actor_email`).
- Endpoint `GET /api/admin/system-log/event-types` liefert die zulässigen Enum-Werte für Kategorie-, Severity- und Event-Type-Filter.
- Erfasste Events: `LOGIN_SUCCESS`/`LOGIN_FAILED`/`LOGOUT`, `PASSWORD_RESET_REQUESTED`/`PASSWORD_RESET_COMPLETED`/`PASSWORD_CHANGED`, `USER_CREATED`/`USER_UPDATED`/`USER_DELETED`/`USER_ROLES_CHANGED`, `ROLE_CREATED`/`ROLE_UPDATED`/`ROLE_DELETED`, `HYGIENE_CERTIFICATE_APPROVED`/`HYGIENE_CERTIFICATE_REJECTED`, `PARTNER_APPLICATION_APPROVED`/`PARTNER_APPLICATION_REJECTED`, `STORE_DELETED`/`STORE_RESTORED`/`STORE_MEMBER_ASSIGNED`, `MAIL_DELIVERY_FAILED`, `UNHANDLED_EXCEPTION`. Veröffentlicht via `ApplicationEventPublisher`, persistiert vom synchronen `SystemLogEventListener` in einer `REQUIRES_NEW`-Transaktion (auch fehlgeschlagene Logins werden so gespeichert).
- IP-Adresse (mit `X-Forwarded-For`-Beachtung) und `User-Agent` werden aus dem aktuellen HTTP-Request automatisch ergänzt.
- `GlobalExceptionHandler` (`@RestControllerAdvice` mit niedrigster Priorität) fängt nicht behandelte Exceptions, schreibt sie als `UNHANDLED_EXCEPTION` (mit URI + Stacktrace, max. 4000 Zeichen) und liefert generisches `500`.
- Auto-Cleanup-Job `SystemLogCleanupJob` löscht Einträge älter als `systemlog.retention-days` (Default 90) per Cron `systemlog.cleanup-cron` (Default `0 0 3 * * *`). Beide Properties via Umgebungsvariablen `SYSTEMLOG_RETENTION_DAYS` und `SYSTEMLOG_CLEANUP_CRON` überschreibbar; `retention-days <= 0` deaktiviert den Cleanup.

## [0.13.1] - 2026-05-08

### Fixed

- `GET /api/hygiene-certificates` ohne `status`-Param lieferte nur `PENDING`-Einträge zurück (Default im Service). Ohne Filter werden jetzt alle Zertifikate per `findAllByOrderByCreatedAtAsc` zurückgegeben — der „Alle"-Filter in `/admin/zertifikate` zeigt damit auch genehmigte und abgelehnte Einträge.

## [0.13.0] - 2026-05-08

### Added

- Passwort-Reset-Flow: Neue Endpoints `POST /api/auth/forgot-password` (Body `{email}`, antwortet immer `204` — keine User-Enumeration) und `POST /api/auth/reset-password` (Body `{token, newPassword}`, `204` bei Erfolg, `400` bei ungültigem/abgelaufenem Token). Token-Lebensdauer 30 Min, vorhandene Tokens des Users werden bei einer neuen Anfrage gelöscht. Beim erfolgreichen Reset werden alle Refresh-Tokens des Users widerrufen. Mail enthält Link auf `${app.frontend.base-url}/reset-password/{token}`.
- Liquibase-Changeset 016 legt Tabelle `password_reset_token` (Hash, `expires_at`, `used_at`, FK auf `app_user` mit `ON DELETE CASCADE`) und Index auf `user_id` an.
- `AppProperties` (`prefix=app`, Sub-Record `Frontend(baseUrl)`); neue Property `app.frontend.base-url` (Default `http://localhost:4200`, Override via `FRONTEND_BASE_URL`) für Link-Erzeugung in Mails.

### Changed

- `MailService.sendHtml` versendet HTML jetzt als Multipart-Mail mit zusätzlichem Plain-Text-Part — entweder explizit übergeben (`sendHtml(to, subject, html, plain)`) oder automatisch aus dem HTML abgeleitet. Verbessert Spam-Score und Lesbarkeit in Text-only-Clients.

## [0.12.0] - 2026-05-08

### Added

- Hygienezertifikat-Upload als Voraussetzung für die Retter-Rolle: User können über `POST /api/users/{id}/hygiene-certificate` (multipart `file` + Query-Param `issuedDate`, ISO-Datum) ein Zertifikat (PDF / JPG / PNG / WebP, max. 10 MB) hochladen. Pro User existiert maximal ein Eintrag (`HygieneCertificateEntity`, `@OneToOne` mit Unique-Constraint auf `user_id`); Re-Upload löscht die alte Datei von der Disk und setzt den Status auf `PENDING`. Eigentümer und ADMINISTRATOR/TEAMLEITER können Metadaten via `GET /api/users/{id}/hygiene-certificate` und die Datei per Streaming via `GET /api/users/{id}/hygiene-certificate/file` (auth-geschützt, kein öffentliches Static-Mapping) abrufen. ADMINISTRATOR/TEAMLEITER prüfen Zertifikate über `GET /api/hygiene-certificates?status=…` und `…/pending-count`, genehmigen via `POST /api/hygiene-certificates/{id}/approve` (vergibt Rolle `RETTER`, entfernt `NEW_MEMBER`) oder lehnen via `POST /api/hygiene-certificates/{id}/reject` (Body `{reason}`, Pflicht) ab.
- Neuer Service `DocumentStorageService` (Paket `storage`) parallel zu `ImageStorageService`: erlaubt PDF + Bilder, max. 10 MB, Ablage unter `uploads/certificates/<id>-<uuid>.<ext>`, mit `resolve(relativePath)` für authentifiziertes File-Streaming und `delete(relativePath)`.
- Liquibase-Changeset 015 legt Tabelle `hygiene_certificate` an (FK auf `app_user` mit `ON DELETE CASCADE`, Unique-Constraint auf `user_id`, FK auf `decided_by_user_id` mit `ON DELETE SET NULL`, Status-Index).
- SSE-Notifications: `HYGIENE_CERTIFICATE_SUBMITTED` an Admin/Teamleitung beim Upload, `HYGIENE_CERTIFICATE_APPROVED`/`HYGIENE_CERTIFICATE_REJECTED` an den User nach Entscheidung.

### Changed

- `WebMvcConfig`: Static-Resource-Mapping unter `/uploads/**` ist nicht mehr pauschal öffentlich, sondern explizit auf die öffentlichen Subdirs `logos/` und `photos/` beschränkt. `uploads/certificates/` ist nicht direkt erreichbar — Zertifikate werden ausschließlich über den authentifizierten Endpoint ausgeliefert.

## [0.11.0] - 2026-05-08

### Added

- Logo-Upload für Betriebe: neuer Endpoint `POST /api/partners/{id}/logo` (multipart, Feld `file`) speichert hochgeladene Bilddateien (JPG/PNG/WebP/GIF, max. 5 MB) unter `uploads/logos/<id>-<uuid>.<ext>`, schreibt den öffentlichen Pfad in `Partner.logoUrl` und löscht eine zuvor lokal gespeicherte Datei.

### Changed

- `PhotoStorageService` (Paket `user`) wurde generalisiert und nach `de.tellerstatttonne.backend.storage.ImageStorageService` verschoben. Signatur jetzt `store(String subdir, String idPrefix, MultipartFile file, String previousUrl)`. `UserController.uploadPhoto` ruft den Service mit `subdir = "photos"` auf — Verhalten und URL-Format (`/uploads/photos/<id>-<uuid>.<ext>`) unverändert.

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
