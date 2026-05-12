# Changelog — Frontend

Alle nennenswerten Änderungen am Frontend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

## [0.23.1] - 2026-05-12

### Added

- Im Event-Editor (Bearbeiten-Modus) gibt es jetzt eine Sektion „Abhol-Termine": Liste aller Pickups der Veranstaltung (Datum, Zeitraum, Kapazität, Anzahl Eingetragener) mit Lösch-Button, plus Inline-Formular zum Anlegen weiterer Termine (Datum, Start/Ende, Kapazität). Validiert Reihenfolge der Zeiten und prüft, dass der Termin innerhalb der Veranstaltungs-Laufzeit liegt. Termine mit bereits eingetragenen Retter:innen können nicht gelöscht werden.

## [0.23.0] - 2026-05-11

### Added

- Veranstaltungen als kurzlebige Abholungsorte: neues Modul `events` mit `event.model.ts`, `event.service.ts`, `events-list/` (Tabs „Aktiv" / „Vergangen") und `event-form/` (Anlegen und Bearbeiten von Name, Beschreibung, Start-/Enddatum, Adresse, Ansprechperson, optionalem Logo). Routen `/events`, `/events/new`, `/events/:id` (Bearbeiten/Anlegen nur für `ADMINISTRATOR`/`TEAMLEITER`).
- Top-Navigation: neuer Eintrag „Veranstaltungen" für alle eingeloggten Rollen.
- Karte: aktive Veranstaltungen erscheinen mit eigenem `event`-Marker neben Betrieben und Verteilerplätzen; Popups zeigen Name, Zeitraum und Adresse.

### Changed

- `Pickup`-Modell und `DaySlot`-Modell um `eventId`, `eventName`, `eventLogoUrl` erweitert. Dashboard zeigt Event-Pickups jeder/jedem Retter:in (offen, ohne Vorab-Zuordnung) — die Anzeige richtet sich nach dem neuen Diskriminator: ist `eventId` gesetzt, wird der Slot als Veranstaltung dargestellt.

## [0.22.0] - 2026-05-11

### Added

- Zentraler Signal-basierter `PartnerCategoryRegistry` in `frontend/src/app/partners/`: lädt aktive Betrieb-Kategorien einmalig per `APP_INITIALIZER` aus `/api/partner-categories` und stellt sie als Signal sowie über `labelForId(id)`, `iconForId(id)`, `byId(id)`, `byCode(code)` allen Konsumenten bereit. Damit gibt es keine hartcodierten Kategorien mehr im Frontend.
- Admin-View „Betrieb-Kategorien" unter `/admin/partner-categories`: Tabelle mit Aktiv-Toggle und Löschen sowie Formular zum Anlegen/Bearbeiten (Code, Label, Reihenfolge, Aktiv-Flag) inkl. Icon-Picker-Grid aus kuratierter Liste lebensmittelbezogener Material-Symbols. Eintrag im Admin-Dashboard.

### Changed

- `Partner.category` (String-Literal-Union) wurde durch `Partner.categoryId: number | null` ersetzt; die statischen Maps `CATEGORY_LABELS` und `CATEGORY_ICONS` sowie der `Category`-Type aus `partner.model.ts` sind entfallen. Karte, Stores-Liste, Pickup-Card, Dashboard, Store-Detail-Dialog, Mitglieder-Verwaltung und Papierkorb laden Labels/Icons jetzt über den `PartnerCategoryRegistry`.
- `Pickup.partnerCategory` und `DaySlot.partnerCategory` heißen jetzt `partnerCategoryId: number | null` (passend zum Backend).

## [0.21.0] - 2026-05-11

### Added

- Admin-View „Verteilerplätze" (öffentlich „Teller-Treff") unter `/admin/distribution-points`: Liste plus Formular zum Anlegen und Bearbeiten. Pflichtfeld Name; optionale Beschreibung, Adresse (Straße, PLZ, Stadt, Lat/Lng), Mehrfach-Auswahl von Betreiber:innen und beliebig viele Öffnungszeiten-Slots (Wochentag + Von/Bis als `<input type="time">`). Löschen über eigenen Confirm-Dialog. Zugriff für `ADMINISTRATOR` und `TEAMLEITER`.
- Karte „Verteilerplätze" im Admin-Dashboard.

## [0.20.0] - 2026-05-11

### Added

- Neue Betriebs-Kategorie `BUTCHER` (UI-Label „Metzgerei", Icon `kebab_dining`). Auswählbar im Betriebs-Edit, in den Kartenfiltern und korrekt dargestellt in Pickup-Cards.

## [0.19.0] - 2026-05-08

### Added

- Erfolgs-Hinweis beim Anlegen eines Nutzers: nach dem Speichern erscheint ein Banner „Eine Einladungs-Mail wurde an {email} gesendet."
- Button „Einladung erneut senden" im User-Edit (sichtbar für `ADMINISTRATOR`/`TEAMLEITER`, solange der Nutzer im Status `PENDING` ist und noch kein Passwort gesetzt hat). Bestätigung läuft über `ConfirmDialogService`.
- `UserService.resendInvitation` und `User.hasPassword` ergänzt.

### Changed

- Anlage-Formular für Nutzer enthält kein Passwort-Feld mehr — der angelegte Nutzer vergibt sein Passwort selbst über den Einladungs-Link aus der zugesendeten E-Mail.

## [0.18.0] - 2026-05-08

### Added

- Erweiterter User-Status: `UserStatus` umfasst jetzt `PENDING`, `ACTIVE`, `PAUSED`, `LEFT` und `REMOVED`. Im User-Edit zeigt ein Statusabschnitt ein Status-Badge, bei `PENDING` zusätzlich den Onboarding-Fortschritt mit Häkchen für Einführungsgespräch und Hygienezertifikat.
- Kontextabhängige Aktions-Buttons im User-Edit (alle über die eigene `ConfirmDialogService`): „Einführung bestätigen", „Pausieren", „Reaktivieren", „Austreten", „Entfernen". Die Buttons rufen die neuen Endpoints `POST /api/users/{id}/introduction-completed`, `/pause`, `/reactivate`, `/leave`, `/remove` auf.
- `UserService` (Frontend) mit neuen Methoden `markIntroductionCompleted`, `pause`, `reactivate`, `leave`, `remove`. `User` hat zwei neue Felder `introductionCompletedAt` und `hygieneApproved`.

### Changed

- Status-Dropdown im User-Edit-Formular entfernt — Status-Wechsel laufen ausschließlich über die dedizierten Aktions-Buttons.

## [0.17.0] - 2026-05-08

### Added

- Admin-Seite `/admin/system-log` (`roleGuard(['ADMINISTRATOR'])`) zeigt das Backend-Systemlog read-only an. Tabelle mit Spalten Zeitpunkt, Severity (Badge: INFO grau, WARN tertiary, ERROR rot), Kategorie, Event-Typ, Akteur (E-Mail), Meldung, IP. Klick auf eine Zeile blendet Detail-Bereich (Ziel, User-Agent, Details/JSON) ein.
- Filterleiste: Kategorie, Event-Typ, Severity, Datums-Bereich (von/bis als `<input type="date">`), Volltextsuche über Meldung und Akteur-E-Mail. Pagination mit Zurück/Weiter und 50 Einträgen pro Seite.
- Neue Karte „Systemlog" auf `/admin`.
- `SystemLogService` mit `list(filter, page, size)` und `metadata()` (lädt Enum-Werte für Filter-Dropdowns).

## [0.16.0] - 2026-05-08

### Added

- Passwort-Reset-Flow: Neue Seiten `/forgot-password` (Mail-Eingabe, sendet an `POST /api/auth/forgot-password`, generischer Erfolgs-Hinweis) und `/reset-password/:token` (neues Passwort + Bestätigung mit Cross-Field-Validator, sendet an `POST /api/auth/reset-password`, Redirect auf `/login` nach Erfolg). Login-Seite zeigt zusätzlich den Link "Passwort vergessen?".
- `AuthService.forgotPassword(email)` und `AuthService.resetPassword(token, newPassword)`.

## [0.15.0] - 2026-05-08

### Added

- Hygienezertifikat-Bereich im Profil (`/profile`): User laden ihr Zertifikat (PDF, JPG, PNG oder WebP, max. 10 MB) zusammen mit dem Ausstellungsdatum hoch. Status-Badge (`Ausstehend` / `Genehmigt` / `Abgelehnt`), Vorschau-Button (öffnet das authentifiziert geladene Dokument als Blob in einem neuen Tab) und — bei Ablehnung — Begründung sind sichtbar. Hinweistext für User ohne `RETTER`-Rolle erklärt den Pflichtcharakter. Beim erneuten Hochladen wird das alte Dokument ersetzt und der Status zurück auf `PENDING` gesetzt.
- Review-Oberfläche `/admin/zertifikate` für Admin und Teamleitung mit Status-Filter (Offen/Genehmigt/Abgelehnt/Alle), eingebetteter PDF-Vorschau (sicher gebunden via `DomSanitizer`) bzw. Bild-Vorschau, Genehmigen-Button (Bestätigung über bestehende `ConfirmDialogService`-Komponente) und Inline-Pflicht-Textarea für die Ablehnungsbegründung. Tile in der Admin-Übersicht ergänzt.
- `HygieneCertificateService` (`getForUser`, `upload`, `fetchFile`, `list`, `pendingCount`, `approve`, `reject`) plus Modell mit Status-Labels.

## [0.14.1] - 2026-05-08

### Added

- Logo-Upload im Betrieb-Formular (`/stores/edit/:id`): zusätzlich zum bestehenden URL-Feld lassen sich Logos jetzt direkt hochladen (JPG/PNG/WebP/GIF, max. 5 MB). Erfolgreicher Upload setzt das `logoUrl`-Feld auf den vom Backend gelieferten Pfad. Der Upload-Button ist erst nach dem ersten Speichern eines neuen Betriebs verfügbar.
- `PartnerService.uploadLogo(id, file)` (POST `/api/partners/{id}/logo` als multipart).

### Fixed

- Logo-Anzeige in Betriebs-Liste, Detail-Dialog, Dashboard und Mitglieder-Verwaltung nutzt jetzt die `photoUrl`-Pipe, sodass relative Pfade aus Uploads (`/uploads/logos/...`) korrekt mit Backend-Host aufgelöst werden.

## [0.14.1] - 2026-05-08

### Changed

- Landing-Page: „Wer wir sind"-Panel auf eine Icon-Liste mit vier Punkten umgestellt (Verein-Selbstvorstellung, Erfahrung & Ziel, Zuverlässigkeit/Hygiene, Verteil-Anspruch) — analog zu den bestehenden Panels „Unsere Ziele" und „Was wir tun".

## [0.14.0] - 2026-05-07

### Added

- Pro Slot eines Betriebs (`/stores/edit/:id` → Abholzeiten) lässt sich jetzt eine erwartete Menge in Kilogramm pflegen (optional, leer erlaubt). Wert wird beim Anlegen einer neuen Abholung als Default für „Gerettet (kg)" übernommen und kann dort überschrieben werden.
- Neue Route `/statistik` (Admin/Teamleitung): zeigt insgesamt gerettete Kilogramm, Anzahl abgeschlossener Abholungen sowie Top-10-Betriebe und Top-10-Retter. Navigation in der Seitenleiste neben „Abholung-Planer".
- Neuer `StatsService` (`/api/stats/overview`).

## [0.13.0] - 2026-05-07

### Added

- Bewerbungen auf Betriebe: In der Betriebe-Übersicht (`/stores`) erscheint für Retter und neue Mitglieder pro Betriebs-Karte ein „Bewerben"-Button. Bei bereits laufender Bewerbung steht „Bewerbung offen", bei bestehender Mitgliedschaft „Mein Betrieb" — der Button erscheint dann nicht. Dialog `apply-to-store-dialog` mit optionaler Nachricht (max. 1000 Zeichen). Neue Route `/my-applications` für die Retter-Sicht (Status, Begründung, Zurückziehen). Neue Route `/admin/applications` für Admin/Teamleitung mit Status-Filter, Annehmen-/Ablehnen-Buttons und Begründungs-Textfeld bei Ablehnung. Karte „Bewerbungen" auf dem Admin-Dashboard. Hinweis-Sektion auf dem Retter-Dashboard mit Direkt-Links für `NEW_MEMBER`. Neuer `PartnerApplicationsService`.

### Changed

- Browser-`window.confirm`-Aufrufe (7 Stellen) durch eigene `ConfirmDialogComponent` ersetzt — eingebunden global in `app.html`, gesteuert über `ConfirmDialogService.ask({title, message, confirmLabel, cancelLabel, tone})`. Betroffen: Bewerbung annehmen/ablehnen/zurückziehen, Nutzer löschen, Quiz-Bewerber entsperren, Quiz-Frage löschen, Quiz-Kategorie löschen, Notiz entfernen.
- Begriff „Botschafter" durchgängig in „Teamleiter/Teamleitung" umbenannt (Rolle, UI-Texte, Methodennamen wie `isAdminOrTeamleiter`, Role-Guards und Badge-Klassen). Sichtbar in Dashboard-Hint, Privacy-Seite, Nutzerliste, Nutzer-Edit, Notizen-Komponente.

## [0.12.0] - 2026-05-07

### Added

- Filterleiste in der Betriebe-Übersicht (`/stores`): Textsuche über Name/Straße/PLZ/Stadt sowie Dropdown-Filter für Kategorie und Kooperationsstatus. Filter wirken clientseitig auf die geladene Liste und kombinieren sich UND-verknüpft; ein „Filter zurücksetzen"-Button erscheint, sobald ein Filter aktiv ist. Bei aktivem Filter zeigt die Kopfzeile zusätzlich `X von Y Betriebe`. Eigener Empty-State, wenn kein Treffer übrig bleibt.

### Removed

- Statische „Interactive Map"-Promo-Kachel am Ende der Betriebe-Übersicht (verlinkte ohnehin nur auf `/map`, deren Eintrag in der Sidebar weiterhin existiert). Die ungenutzte `mapImage`-Konstante in `stores.ts` wurde mitentfernt.

## [0.11.0] - 2026-05-07

### Added

- Notiz-Sektion im Betrieb-Editor (`PartnerNotesSectionComponent`, neben dem Hauptansprechpartner-Block): Append-only Notizverlauf pro Betrieb mit Eingabefeld, Sichtbarkeits-Toggle (`Sichtbar für alle` / `Intern`) für Botschafter & Admin, Lösch-Button (Soft-Delete) für Botschafter & Admin. Retter sehen nur eigene Notizen plus alle als `Sichtbar für alle` markierten und können selbst nur sichtbar-für-alle posten. Neuer `PartnerNotesService` ruft `/api/partners/{id}/notes` auf.

## [0.10.0] - 2026-05-07

### Changed

- **Breaking:** `Partner.Status` ersetzt das bisherige Lifecycle-Enum (`ACTIVE`/`INACTIVE`/`DELETED`) durch sieben deutsche Kooperationsstatus (`KEIN_KONTAKT` als Default, `VERHANDLUNGEN_LAUFEN`, `WILL_NICHT_KOOPERIEREN`, `KOOPERIERT`, `KOOPERIERT_FOODSHARING`, `SPENDET_AN_TAFEL`, `EXISTIERT_NICHT_MEHR`). Neue `STATUS_LABELS`/`STATUS_ORDER`-Exports in `partner.model.ts`. Status-Dropdown in `partner-edit` listet alle sieben Werte; Status-Badge in Stores-Liste, Detail-Dialog und Karten-Popup zeigt das deutsche Label. `partner-edit.isDeleted` und `map`-Filter (`activeOnly`, Marker-Style, `findAllDeleted`-Konsumenten) prüfen jetzt `KOOPERIERT` bzw. `EXISTIERT_NICHT_MEHR`.
- Sichtbare UI-Texte verwenden ab sofort die deutsche Bezeichnung **Betrieb / Betriebe** statt "Store(s)" oder "Partner". Betroffen: Sidebar, Mobile-Navigation, Stores-Übersicht, Betrieb-Editor, Betriebsdetails, Mitglieder-↔-Betriebe-Verwaltung, Papierkorb, Pickup-Editor, Karte, Dashboard, Verfügbarkeiten, Datenschutzerklärung. Code-Pfade (`stores/`, `partners/`) und API-Endpoints bleiben unverändert.

## [0.9.1] - 2026-05-07

### Fixed

- Benachrichtigungs-Dropdown ragte über den linken Viewport-Rand hinaus, weil es per `absolute right-0` an der Glocke ankerte (die selbst nicht am rechten Rand sitzt). Das Panel ist jetzt grundsätzlich `fixed top-16 right-2`, am Viewport ausgerichtet: auf Mobile spannt es zwischen `left-2` und `right-2`, ab `md` ist es 320 px breit und am rechten Viewport-Rand verankert.

## [0.9.0] - 2026-05-07

### Added

- Nachrichtensystem (Phase 1): neue `NotificationService` (Signals `notifications`, `unreadCount`) und `NotificationBellComponent` ersetzen den statischen Glocken-Block in `app-shell.html`. Der Service lädt initial via `GET /api/notifications`, verbindet sich mit `GET /api/notifications/stream` über fetch+ReadableStream (mit `Authorization`-Header) und fängt `notification`/`unread-count`-Events live ab; bei Verbindungsabbruch reconnectet er mit Backoff (1/2/5/10/30 s). Glocke zeigt Unread-Badge, Dropdown listet die letzten 10 Nachrichten mit relativem Zeitstempel; Klick markiert als gelesen und navigiert (falls vorhanden) zur zugehörigen Pickup-Detailseite. „Alle gelesen"-Button für Massen-Mark-Read. Stream-Lifecycle ist in `app-shell` an `auth.isAuthenticated()` gekoppelt.

## [0.8.10] - 2026-05-07

### Fixed

- Store-Detail-Dialog erschien hinter der Leaflet-Karte (deren Popup-Pane bei `z-index: 700` liegt). Z-Index des Dialog-Overlays auf `z-[1200]` erhöht, damit der Dialog auch beim Öffnen aus dem Map-Popup vor der Karte und über der mobilen Navigation liegt.

## [0.8.9] - 2026-05-07

### Changed

- Map-Popup: Retter sehen statt „Bearbeiten →" einen Link „Details ansehen →", der den schreibgeschützten Store-Detail-Dialog öffnet. Administratoren und Botschafter sehen weiterhin den Bearbeiten-Link.

## [0.8.8] - 2026-05-07

### Changed

- Map-Popup: Der „Bearbeiten →"-Link wird für Nutzer mit der Rolle `RETTER` ausgeblendet. Retter sehen im Popup nur die Store-Details (Name, Status, Kategorie, Adresse). Administratoren und Botschafter sehen den Link unverändert.

## [0.8.7] - 2026-05-07

### Changed

- Pickup-Karte: Der „Austragen"-Button wird ab 2 Stunden vor Pickup-Beginn ausgegraut und deaktiviert; ein Tooltip erklärt „Austragen ist nur bis 2 Stunden vor Beginn möglich.". Bei direktem Austragungs-Versuch (HTTP 422) wird ein passender Fehlertext in der Wochenansicht angezeigt.

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
