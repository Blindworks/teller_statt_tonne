# Changelog — Frontend

Alle nennenswerten Änderungen am Frontend werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
und das Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

## [Unreleased]

### Added

- Eigener „Ansprechpartner für Retter“ pro Betrieb (Name, E-Mail, Telefon). Erfassung im Betrieb-Bearbeiten-Formular (`partner-edit`) als neuer Abschnitt unter dem Hauptansprechpartner mit gleicher Form-Struktur (`retterContact`-FormGroup, E-Mail-Validator). Anzeige für Retter im Betriebsdetails-Dialog (`store-detail-dialog`) und im Pickup-Run „Hinweise zum Betrieb“ (`/pickups/:id/run`, Schritt 1): Name, klickbarer `mailto:`-Link und `tel:`-Link. Bedingt eingeblendet — der Block erscheint nur, wenn mindestens ein Feld gefüllt ist. `onSiteContactNote` bleibt als ergänzende Freitext-Notiz erhalten (Label im Pickup-Run umbenannt auf „Hinweis zum Ansprechpartner“). `Partner.retterContact` ist Pflichtfeld im Model (`Contact`-Interface wiederverwendet); `emptyPartner()` initialisiert leeren Kontakt.
- Administrative Aktionen zum Sperren und zum Zurücksetzen in den Onboarding-Status in der Nutzer-Detailansicht (`user-edit`). Drei neue Aktions-Buttons im Abschnitt „Mitgliedsstatus" (nur sichtbar für `ADMINISTRATOR`):
  - **Sperren** — sichtbar in den Status `PENDING`/`ACTIVE`/`PAUSED`. Bestätigt über `ConfirmDialogService` (kein `window.confirm`), ruft `POST /api/users/{id}/lock` auf. Der Nutzer kann sich danach nicht mehr einloggen und bestehende Sessions werden sofort ungültig.
  - **Entsperren** — sichtbar im Status `LOCKED`, ruft `POST /api/users/{id}/unlock` auf (zurück nach `ACTIVE`).
  - **In Onboarding zurücksetzen** — sichtbar in `ACTIVE`/`LOCKED`/`PAUSED`. Bestätigt über `ConfirmDialogService`, ruft `POST /api/users/{id}/reset-to-onboarding` auf. Onboarding-Timestamps und Hygienezertifikat bleiben erhalten.
- Status `LOCKED` mit Label „Gesperrt" in `UserStatus`, `USER_STATUSES`, `USER_STATUS_LABELS` und im Lifecycle-Stepper. `statusBadgeClass` rendert `LOCKED` im Error-Container-Farbschema. „Austreten" und „Entfernen" sind zusätzlich aus dem Status `LOCKED` heraus erreichbar.
- `UserService.lock(id)`, `unlock(id)`, `resetToOnboarding(id)` als HTTP-Wrapper für die neuen Backend-Endpoints.
- Nutzerübersicht (`users-list`): gesperrte Nutzer-Kacheln werden mit einem 2px-breiten `border-error`-Rahmen markiert und tragen oben rechts ein rundes Lock-Icon-Badge (`bg-error`/`text-on-error`, Tooltip „Gesperrt").
- Login-Screen zeigt für gesperrte Nutzer eine spezifische Fehlermeldung: „Dein Konto ist gesperrt. Bitte wende dich an einen Administrator." Direkter Login-Versuch eines `LOCKED`-Nutzers erkennt den Backend-Body `Account locked`. Wird ein eingeloggter Nutzer während einer Session gesperrt, scheitert der Token-Refresh in `AuthService.refresh` mit demselben Body — die Komponente persistiert `tst.lockReason=locked` in `sessionStorage`, der Login-Screen liest und löscht das Flag im Konstruktor und zeigt dieselbe Meldung. So beginnen gesperrte Nutzer nicht mehr fälschlich ein Passwort-Reset.

### Fixed

- Betriebsdetails-Dialog auf `/stores` ist für Retter nur noch für **eigene** Betriebe öffnen — bei nicht zugewiesenen Betrieben ist die Karte für Retter nicht mehr klickbar (kein `role=button`, kein `tabindex`, kein `cursor-pointer`, kein Hover-Wechsel; Klick/Keyboard-Event wird in `openDetail()` zusätzlich ignoriert). Für Admin/Teamleiter/Koordinator und für eigene Betriebe von Rettern bleibt das Verhalten unverändert. Neuer Helper `canOpenDetail(partner)` in `stores.ts`. Zusätzlich defensiv gemacht: `partner.contact?.name` in `stores.html`, `activeSlots`/`inactiveSlots` im `StoreDetailDialogComponent` tolerieren `pickupSlots == null`, und das `stopPropagation` der Status-Box wurde gezielt auf den Edit-Link verschoben (Bewerben-Button hat es bereits im Handler).
- Pickup-Run-Screen (`/pickups/:pickupId/run`): Stepper-Kopf und „Hinweise zum Betrieb“-Überschrift waren vom fixierten App-Header verdeckt. Das `<main>` bekommt jetzt `pt-24 md:pl-80` (analog Dashboard), und der interne Sticky-Stepper-Header dockt mit `top-24` unter dem App-Header an.

### Changed

- Sidebar-Navigation um eine Ebene erweitert: „Karte“ und „Mitglieder ↔ Betriebe“ erscheinen jetzt als eingerückte Sub-Items direkt unter „Betriebe“ (visuell durch eine linke vertikale Linie gruppiert). Mobile Bottom-Nav und „Mehr“-Sheet bleiben unverändert.
- Navigations-Eintrag „Benutzer“ (`/users`) ist für Retter nicht mehr sichtbar — weder in der Desktop-Sidebar noch im mobilen „Mehr“-Bottom-Sheet. Neue `canSeeUsers`-Sichtbarkeit in `app-shell.ts` prüft auf ADMINISTRATOR/TEAMLEITER/KOORDINATOR (analog zu `canSeeStoreMembers`). Im „Mehr“-Sheet wurde die bisherige `isPlanner`-Kopplung in zwei unabhängige Bedingungen aufgeteilt (`canSeeUsers` für Benutzer, `!isPlanner` für Abholung-Planer).

### Removed

- Dynamisches Berechtigungs-/Feature-Modell vollständig entfernt. Die Dateien `permissions.service.ts`, `feature.guard.ts`, das gesamte Admin-Verzeichnis `admin/permissions/` (Permissions-Matrix, Feature-Form, Feature-Service/Model) sowie die Admin-Karte „Berechtigungen“ und die Route `/admin/permissions` sind weg. `PermissionsService`-Aufrufe in `AuthService`, `AppShellComponent`, `AdminDashboardComponent`, `TeamleiterDashboardComponent` und `TicketFormComponent` entfernt — die initiale `/api/me/features`-HTTP-Last beim Login/Refresh entfällt.

### Added

- Neuer Route-Guard `roleGuard(...rolesAllowed)` (`auth/role.guard.ts`) ersetzt den bisherigen `featureGuard`: prüft `User.roles` direkt gegen eine fixe Liste erlaubter Rollen, await’t beim Page-Reload einmalig `AuthService.ensureUserLoaded()` und redirected auf `/dashboard`. Mapping in `app.routes.ts` jetzt hartkodiert: `route.planner` → ADMINISTRATOR/TEAMLEITER/KOORDINATOR, `route.planner.view` → ADMINISTRATOR/TEAMLEITER/KOORDINATOR/RETTER, `route.user.edit` & `route.quiz.admin` → ADMINISTRATOR/TEAMLEITER, `route.admin` → ADMINISTRATOR.
- KOORDINATOR-Rolle erhält im neuen Mapping Planungs-Schreibrechte sowie Sichtbarkeit von „Retter im Betrieb“ und „Verteilerplätze“ (zwischen RETTER und TEAMLEITER). Kein User-/Quiz-/Admin-Zugriff.
- Helper `hasAnyRole(user, ...roles)` in `users/user.model.ts`.
- `AuthService.ensureUserLoaded(): Promise<User|null>` cached die erste `/api/auth/me`-Antwort nach App-Start für den `roleGuard`.

### Fixed

- Beim Userwechsel im selben Tab (z. B. Admin → Logout → Retter-Login) wurden in der Sidebar kurzzeitig Menüpunkte des vorherigen Users (Administration, Teamleitung, Verteilerplätze, Mitglieder ↔ Betriebe, Tickets, Statistik, Abholung-Planer) angezeigt – sie verschwanden erst nach einem Browser-Reload. Ursachen: (1) `AuthService.login/refresh/impersonateTestUser` lösten `permissions.load()` „fire-and-forget“ aus und navigierten weiter, bevor die Features des neuen Users vom Backend zurück waren; (2) eine ggf. noch laufende `/api/me/features`-Response des vorherigen Users konnte nach `permissions.clear()` das Signal mit alten Features überschreiben (`tap(set => featuresSignal.set(set))`). Fix: Login/Refresh/Impersonation warten jetzt per `switchMap` auf `permissions.load()`, bevor sie die Auth-Response emittieren; `PermissionsService.load()` ignoriert Antworten veralteter Aufrufe über eine monoton steigende Sequenznummer, die in `clear()` mit-inkrementiert wird.
- Menüpunkt „Teamleitung“ ist für Retter nicht mehr sichtbar. Die Sichtbarkeit (`canSeeTeamleitung` in `app-shell.ts`) prüft jetzt das Feature `route.planner` (nur Teamleiter/Admin) statt `nav.planner` (auch Retter, da diese „Abholung-Planer“ und „Statistik“ sehen).

### Added

- Neuer Menüpunkt „Termine” (`/termine`) für Ankündigungen, Meetings und Schulungen mit Rollen-Targeting. Neues Modul `src/app/appointments/` mit Liste (`appointments-list`, Filter Kommend/Vergangen, „Neu”-Markierung für ungelesene), Detailansicht (`appointment-detail`, markiert automatisch als gelesen), Formular (`appointment-form`, nur Admin/Teamleiter – Titel, Start/Ende, Ort oder Online-Link, Beschreibung, Anhang-Link, Multi-Select Rollen, Checkbox „Öffentlich auf Startseite”) und Lösch-Dialog (eigene Komponente, kein `window.confirm`). Bearbeiten/Löschen nur durch Ersteller oder Administrator (server- und clientseitig). Sidebar-Menüpunkt mit Badge für ungelesene Termine und Toast-Banner nach Login bei `unreadCount > 0`. Dashboard-Widget „Kommende Termine” (nächste 5) rechts in der News-Spalte. Öffentliche Section „Kommende Termine” auf der Landing-Page (ruft `/api/public/appointments` anonym; URL-`location` wird als Online-Link gerendert). Neuer Service `AppointmentService` mit `unreadCount`-Signal (lädt nach Login, refresht nach `markRead`).
- Aktion „Direkt auf aktiv setzen” im User-Edit (`/admin/users/:id` und `/teamleitung/users/:id`) für Onboarding-User. Button erscheint nur bei `status === 'PENDING'`, öffnet einen `ConfirmDialog` mit Warnung (Onboarding-Schritte werden übersprungen, Vorgang wird im SystemLog dokumentiert) und ruft `POST /api/users/{id}/force-activate` über `UserService.forceActivate(id)`. Ausnahmeflow für Admin/Teamleiter, falls ein Retter ausnahmsweise ohne reguläres Onboarding aktiviert werden muss.

### Changed

- Dashboard-Sidebar: Status-Kacheln „Nächster Pickup“, „Nächster freier Slot“ und „Offene Slots heute“ bekommen analog zur Kachel „Kommende Termine“ eine Header-Zeile mit farbigem Material-Icon, fettem Titel und optionalem Status-Badge rechts; die zuvor mittig platzierten Icon-Kreise entfallen.
- Menüpunkt „Quiz“ aus Hauptnavigation (Sidebar + mobiles „Mehr“-Sheet) entfernt und als Karte in das Teamleitung-Dashboard (`/teamleitung`) verschoben. Sichtbarkeit weiterhin über Feature `nav.quiz-admin` gesteuert.

### Added

- Rolle „Betriebskoordinator“ (Code `KOORDINATOR`) im Store-Members-Screen (`/admin/store-members`). Betriebsdetail-Ansicht zeigt jetzt zwei getrennte Sektionen — „Koordinatoren“ (oben, Secondary-Farbe, Icon `supervisor_account`) und „Retter“ (unten, Primary-Farbe) — mit jeweils eigenem „Hinzufügen“-Button. `AssignMemberDialog` akzeptiert neuen `title`-Input und wird je nach Sektion mit „Koordinator zuweisen“ bzw. „Retter zuweisen“ aufgerufen; die Auswahlliste ist clientseitig nach Rolle gefiltert (`assignableMembers` berücksichtigt `assignDialogRole`).
- Rollen-Badge-Farbe für `KOORDINATOR` im `user-profile-dialog` (Secondary-Container).

### Changed

- Letzte englische UI-Texte (Dashboard-Hero, Active-Community-Card, Sidebar-Tagline „The Living Pantry" → „Gemeinsam Lebensmittel retten") auf Deutsch umgestellt – App ist jetzt durchgängig deutsch.
- Hygienezertifikat-Upload (Onboarding-Schritt 1 und Profilseite): klarer Abstand zwischen Beschreibungstext und Upload-Formular (`mt-6`), zusätzliche vertikale Luft zwischen den Feldern (`space-y-5`) sowie eine dünne Trennlinie über dem „Zertifikat hochladen“-Button.
- Onboarding-Schritt 4 „Rettervereinbarung“: Button „PDF öffnen“ entfernt (redundant zum Download); mehr Abstand zwischen Beschreibungstext und Aktions-Buttons (`margin-top: 20px` auf `.agreement-actions`).

### Fixed

- Admin-Seiten (`/admin`, `/admin/roles`, `/admin/roles/new|:id`, `/admin/permissions`, `/admin/system-log`): Der fixierte Shell-Header überdeckte Inhalte/Action-Buttons oben rechts. Section-Container an das Muster der übrigen Admin-Listen (`pt-24 pb-32 md:pb-12 px-6 md:pl-80 md:pr-12 max-w-screen-2xl mx-auto min-h-screen`, Role-Form behält `max-w-3xl`) angeglichen.
- Download „Rettervereinbarung herunterladen“ im Onboarding lieferte aufgrund des Service-Worker-Navigations-Fallbacks die Angular-`index.html` (~39 KB `.htm`) statt der PDF. `ngsw-config.json` deckt jetzt `/assets/**` und die Endung `pdf` in der `assets`-Gruppe ab.

### Added

- Ticketsystem `/tickets` (Sidebar-Eintrag „Tickets", sichtbar bei Feature `nav.tickets` für ADMINISTRATOR, TEAMLEITER, RETTER, NEW_MEMBER): Liste mit Filter nach Status/Typ, neues `TicketFormComponent` für Create/Detail/Edit. Pflichtfelder Titel + Typ (BUG/FEATURE), optionale Beschreibung, mehrere Bild-Anhänge per Multi-File-Upload (`ImageStorageService`, Unterordner `tickets`), einfacher Kommentar-Thread. Statuswechsel (`OPEN → IN_PROGRESS → DONE/REJECTED`) sichtbar nur bei Feature `tickets.admin` (ADMINISTRATOR). Ersteller bearbeitet Titel/Beschreibung/Typ/Anhänge solange Status `OPEN`; danach read-only. Löschen von Ticket/Anhang/Kommentar via `ConfirmDialogService` (kein `window.confirm`). Bottom-Sheet auf Mobile zeigt den Eintrag analog.
- Test-Retter im Admin-Onboarding: neue Sektion zum Anlegen, Impersonieren und Löschen von Test-Retter-Accounts. Admin (Rolle `ADMINISTRATOR`) kann mit einem Klick einen Test-Retter erzeugen und sich per Token-Switch als dieser anmelden, ohne den Admin-Login zu verlieren — Admin-Tokens werden in separaten LocalStorage-Slots (`tst.admin_access`/`tst.admin_refresh`) gesichert. Sticky-Banner `app-impersonation-banner` oben in der App zeigt während der Impersonation die Test-User-E-Mail und einen Button "Zurück zum Admin". Neue wiederverwendbare Komponente `app-test-user-badge` markiert Test-Retter in der Pending-Liste, in den Slot-Buchungen und in der Test-Retter-Tabelle. `User`- und `IntroductionBookingInfo`-Modelle erweitert um `testUser: boolean`.
- Admin-Onboarding (`/teamleitung/onboarding`): Kennenlern-Termine als aufklappbare Karten mit Bucher-Chips (Initialen-Avatar + Name). Im aufgeklappten Bereich erscheinen E-Mail und ein Storno-Button pro Bucher; Stornieren läuft über den bestehenden `ConfirmDialogService`.

### Removed

- Button „Einführung bestätigen“ im User-Edit-Menü entfernt; die Bestätigung läuft jetzt ausschließlich über das Admin-Onboarding (`/admin/onboarding`). Die Statuszeile mit Häkchen + Datum bleibt erhalten. Methode `confirmIntroduction()` in `user-edit.ts` entfällt; Service-Methode `markIntroductionCompleted` bleibt verfügbar.

### Added

- Neue Admin-Seite `/admin/system-settings` (Card „Systemeinstellungen“ im Admin-Dashboard) zur Pflege der globalen Hygienezertifikat-Werte: Gültigkeitsdauer (Monate, 1–60) und Vorwarnzeit (Tage, 1–365). Werte synchronisieren mit `GET/PUT /api/admin/settings`.
- Neue Komponente `<app-hygiene-expiry-banner>` (Pickup-Liste, nur für Retter): gelber Hinweis bei nahem Ablauf, roter Banner mit Auffrischungs-CTA bei abgelaufenem Zertifikat.
- Hygienezertifikat-Section zeigt nun „gültig bis“ inkl. Resttage, kennzeichnet abgelaufene/bald-ablaufende Zertifikate und erlaubt Re-Upload auch im Status APPROVED, sobald die Vorwarnzeit erreicht oder das Datum abgelaufen ist.
- Admin-Liste der Hygienezertifikate zeigt zusätzlich „gültig bis“ mit visueller Markierung „abgelaufen“ / „läuft bald ab“.
- Backend-Fehler `hygiene_certificate_expired` (HTTP 403 + Header `X-Reason`) wird in der Pickup-Anmeldung als „Dein Hygienezertifikat ist abgelaufen…“ übersetzt.
- Neue Admin-Seite `/admin/permissions` (Card im Admin-Dashboard) zur rollenbasierten Steuerung der GUI-Sichtbarkeit: Matrix Rolle × Feature mit Checkboxen, „Neues Feature“-Dialog (Key/Label/Kategorie/Beschreibung), Löschen via eigener Bestätigungsdialog. Pro Rolle bzw. global speicherbar. ADMINISTRATOR-Spalte ist gesperrt und immer aktiv.
- Neuer `PermissionsService` (`/api/me/features`) und `featureGuard(key)` lösen den bisherigen `roleGuard([...])`-Mechanismus ab. Navigations-Sichtbarkeit (Sidebar, Bottom-Nav, Admin-Dashboard-Cards) und Route-Guards in `app.routes.ts` arbeiten jetzt gegen Feature-Keys (`nav.*`, `route.*`). `role.guard.ts` entfällt.

### Changed

- Dashboard-Slot „Frei“-Chip trägt Admin/Teamleitung jetzt direkt in den Slot ein (vorher: Weiterleitung zum Abholung-Bearbeiten-Screen). Aria-Label ebenfalls angepasst.
- Dashboard-Sektion für Retter „Freie Slots an deinen Betrieben“ heißt nun „Freie Slots & Sonderabholungen“, damit Retter Sonderabholungen dort tatsächlich erwarten — Inhalt unverändert (`availableSlots()` enthält Events bereits).
- App-Shell (`isAdmin`, `isPlanner`, `canSeeQuizAdmin`, `canSeeStoreMembers`, `canSeeDistributionPoints`, `canSeeTeamleitung`) prüft Feature-Keys statt Rollen-Listen — Verhalten bleibt durch Seed-Daten zunächst identisch.
- Dashboard: Aktions-Reihe (Abholung starten / Austragen / Eintragen) wird nicht mehr nur Rettern, sondern allen Rollen angezeigt, sobald der Slot kein Template ist. Admin/Teamleitung können sich damit auch wieder austragen — gleiches Verhalten für normale Pickups und Sonderabholungen.

- Quiz-Frageseite: Unter dem „Quiz abschicken"-Button steht jetzt ein „Abbrechen"-Link, der das Quiz beendet und zur Startseite navigiert.

### Fixed

- Sidebar-Navigation scrollt jetzt bei kleinen Viewport-Höhen, sodass alle Menüeinträge erreichbar sind.

## [0.28.1] - 2026-05-14

### Changed

- „Papierkorb · Betriebe“ aus der Hauptnavigation entfernt (Desktop-Sidebar und Mobile-„Mehr“-Sheet). Der Bereich ist nun ausschließlich über eine neue Karte im Administration-Dashboard (`/admin`) erreichbar; Route und Komponente bleiben unverändert.

## [0.28.0] - 2026-05-14

### Added

- Neuer Screen „Teamleitung“ unter `/teamleitung` mit den Übersichtskarten Bewerbungen, Onboarding und Hygienezertifikate. Sichtbar für Teamleiter:innen und Administrator:innen. Sidebar- und Mobile-Menü-Eintrag „Teamleitung“ ergänzt.

### Changed

- Die Bereiche Bewerbungen, Onboarding und Hygienezertifikate wurden aus dem Administration-Dashboard (`/admin`) in den neuen Teamleitung-Bereich verschoben. Die URLs lauten jetzt `/teamleitung/applications`, `/teamleitung/onboarding` und `/teamleitung/zertifikate` (vorher `/admin/...`).

## [0.27.2] - 2026-05-14

### Changed

- Datenschutzerklärung um einen neuen Abschnitt „Social-Media-Präsenzen (Facebook, Instagram)“ erweitert: verlinkt unsere beiden Profile, informiert über die Datenverarbeitung durch Meta Platforms Ireland Ltd. (Art. 6 Abs. 1 lit. f DSGVO, gemeinsame Verantwortlichkeit gem. Art. 26 DSGVO für Insights-Daten) und verweist auf die Datenschutzhinweise von Meta. Folgeabschnitte 12–19 entsprechend umnummeriert.

## [0.27.1] - 2026-05-14

### Added

- Quiz-Ergebnisseite: Neben „Quiz neu starten“ steht jetzt ein „Zur Startseite“-Button, der zur Landing-Page navigiert.

## [0.27.0]

### Added

- Onboarding-Seite für noch nicht freigeschaltete Retter unter `/onboarding`: zeigt die fünf Schritte Hygienezertifikat, Kennenlerngespräch (Terminbuchung), Profildaten, Rettervereinbarung (Download/Upload statisches PDF) und Testabholung. Neue Guards (`onboardingRequiredGuard`, `onboardingCompletedGuard`) verhindern Zugriff auf die übrige App, solange der Status `PENDING` ist; die Onboarding-Route liegt ausserhalb der App-Shell (keine Sidebar). Neue Admin-Seite `/admin/onboarding` zur Verwaltung der Kennenlern-Termine, Bestätigung der Teilnahme und manuellem Abhaken der Testabholung.

## [0.26.6] - 2026-05-14

### Changed

- UI-weite Umbenennung: „Veranstaltungen“ heisst überall in der Oberfläche jetzt „Sonderabholungen“ — Menü-Eintrag links, Seitentitel und Leerzustand der Events-Liste, Lösch-Dialog, „Neue Sonderabholung“-Button, Event-Formular-Header und Slot-Hinweistexte, Karten-Toggle und Marker-Popup-Badge, Dashboard-Badge sowie Pickup-Card-Chip. Code-Bezeichner (`events`, `event`, `EventService`, Routen `/events`, Marker-Typ `event`) bleiben unverändert.

## [0.26.5] - 2026-05-13

### Fixed

- Betriebs-Übersicht (`/stores`): Kacheln zeigten konstant `0 Retter`. Ursache: Anzeige basierte auf der Summe von `pickupSlot.availableMemberCount` (Retter mit passender `UserAvailability`), die in der Praxis stets 0 ist. Kachel zählt jetzt die dem Betrieb zugeordneten Mitglieder via `GET /api/partners/member-counts`. Slot-Granularität im Detail-Dialog und im Edit-Formular bleibt unverändert.

## [0.26.4] - 2026-05-13

### Fixed

- Öffentliche Routen (`/reset-password/:token`, `/forgot-password`, `/about`, `/impressum`, `/datenschutz`, `/quiz`) leiteten unangemeldete Besucher fälschlich auf `/login?returnUrl=%2F` um. Ursache: Der `provideAppInitializer` lädt beim App-Start `/api/partner-categories`, das im Backend `authenticated()` ist; der 401 löste im `authInterceptor` einen Hard-Redirect auf `/login` aus. Interceptor unterdrückt den Redirect jetzt, solange der Browser auf einer der öffentlichen Routen steht — `clearSession` und Error-Propagation bleiben unverändert.

## [0.26.3] - 2026-05-13

### Added

- Admin-View `/admin/quiz/attempts`: Quiz-Versuche können jetzt gelöscht werden. Lösch-Button im Detail-Modal sowie ein Mülleimer-Icon je Eintrag in der Versuchsliste. Bestätigungs-Dialog (Tone `danger`) vor dem Löschen; nach erfolgreichem Löschen verschwindet der Versuch aus der Liste und der Bewerber-Status wird neu geladen.

## [0.26.2] - 2026-05-12

### Added

- Betriebsdetails-Dialog zeigt die vier Hinweis-Felder (Parken, Zugang, Vorgehen, Ansprechpartner) in einem neuen Abschnitt **„Hinweise"**, sofern gepflegt.

## [0.26.1] - 2026-05-12

### Changed

- Retter-Dashboard: Eingetragene Pickups, deren Endzeit bereits in der Vergangenheit liegt, erscheinen nicht mehr unter „Meine kommenden Pickups", sondern in einer neuen Sektion **„Vergangene Pickups"** ganz unten auf der Seite (absteigend sortiert, nur sichtbar wenn vorhanden). Die `range`-Abfrage des Dashboards umfasst dafür die letzten 7 Tage zusätzlich. „Freie Slots" und „Belegte Slots" werden zusätzlich auf zukünftige Slots gefiltert. Der Button **„Abholung starten"** erscheint bei vergangenen Pickups nicht mehr.

## [0.26.0] - 2026-05-12

### Added

- **Pickup-Run-Wizard für Retter** (`/pickups/:pickupId/run`, neu): geführter 3-Schritt-Ablauf — (1) Betriebs-Hinweise, (2) Schnellerfassung der geretteten Lebensmittel über Emoji-Tap-Buttons + Freitext-Tag, (3) Verteiler-Wahl + optionale Fotos und Notiz. Beim Abschließen wird automatisch ein Verteiler-Post veröffentlicht. Abbrechen via `ConfirmDialogService`.
- Im Dashboard erscheint bei eigenen heutigen Abholungen der Button **„Abholung starten"**, der direkt in den Wizard navigiert.
- Admin-Maske **Lebensmittel-Kategorien** unter `/admin/food-categories` mit Inline-Editor (Name, Emoji, Farbe, Reihenfolge, Aktiv-Toggle).
- Betriebs-Edit (`/stores/edit/:id`): neuer Abschnitt **Hinweise für Retter** (Parken, Zugang, Vorgehen, Ansprechpartner) sowie **Bevorzugte Lebensmittel** (Toggle-Chips aus Master-Liste).
- Neue Services/Modelle: `FoodCategoryService`, `PickupRunService` mit Models in `pickup-run/pickup-run.model.ts`.

### Changed

- `Partner`-Model erweitert um `parkingInfo`, `accessInstructions`, `pickupProcedure`, `onSiteContactNote`, `preferredFoodCategoryIds`.

## [0.25.2] - 2026-05-12

### Added

- Karte (`/map`): Toggle-Button „Veranstaltungen" im Filterbereich, mit dem die Veranstaltungs-Marker (und Bounds-Fit) ein- bzw. ausgeblendet werden können — analog zum „Verteilerplätze"-Toggle, jedoch für alle Rollen sichtbar. Button und Marker-Pin verwenden die sekundäre Akzentfarbe, damit Veranstaltungen visuell von Verteilerplätzen (tertiär) unterscheidbar bleiben.

## [0.25.1] - 2026-05-12

### Added

- Veranstaltungs-Edit: Button „Adresse verorten" startet Forward-Geocoding für die eingegebenen Adressfelder (Straße/PLZ/Ort) über `GET /api/geocoding/forward` und setzt die ermittelten Koordinaten als Pin. Bei leeren Adressfeldern oder erfolglosem Lookup wird eine Fehlermeldung angezeigt.
- `PartnerService.forwardGeocode(street, postalCode, city)` als wiederverwendbarer Client für den Forward-Endpoint.

## [0.25.0] - 2026-05-12

### Added

- Veranstaltungs-Edit (`event-form`): Lokalität ist jetzt Pflicht. Nutzer:innen können entweder Adresse (mindestens Ort) eingeben **oder** einen Pin auf der Karte setzen. Der wiederverwendete `LocationPickerDialogComponent` öffnet eine Leaflet-Karte; Reverse-Geocoding füllt Straße/PLZ/Ort automatisch beim Setzen des Pins. Gesetzter Pin wird mit Koordinaten-Anzeige und „Pin entfernen"-Button dargestellt.

## [0.24.1] - 2026-05-12

### Changed

- Pickup-Karten im Pickups-View und Slot-Listenzeilen im Dashboard zeigen für Termine, die zu einer Veranstaltung gehören (`eventId` gesetzt), das Badge „Veranstaltung", den Namen der Veranstaltung als Titel, gegebenenfalls das Veranstaltungs-Logo und ein Event-Icon — statt der Partner-Kategorie und „Unbekannt".

## [0.24.0] - 2026-05-12

### Added

- Eigenes Profil (`/profil`): Neue Sektion „Rolle" (bzw. „Rollen" bei mehreren) zeigt die eigene Rolle als farbiges Badge an. Labels stammen aus `/users/roles`, Badge-Farben passen zur Nutzerliste (Administrator/Teamleiter/Retter/Neu).

## [0.23.4] - 2026-05-12

### Fixed

- Nutzerliste: Edit-Button in der Nutzer-Karte ist jetzt wieder kreisrund. Vorher führte das reine `p-2 rounded-full` ohne fixe Maße zu einer leicht ovalen Form, weil das `material-symbols-outlined`-Icon nicht quadratisch ist. Fix: feste Größe `w-10 h-10` mit zentriertem Inhalt.

## [0.23.3] - 2026-05-12

### Changed

- Admin-Quiz-Versuche (`/admin/quiz/attempts`): Der Button „+ Nutzer anlegen" wird ausgegraut und deaktiviert, wenn zur Bewerber-E-Mail bereits ein User-Account existiert. Label wechselt in diesem Fall zu „Nutzer existiert". Verhindert den fehlschlagenden Folge-Versuch mit „Email already registered".

## [0.23.2] - 2026-05-12

### Fixed

- Event-Editor: Container von `max-w-3xl` auf `max-w-5xl` verbreitert, damit die vier nebeneinanderliegenden Eingabefelder im Termin-Anlegen-Formular (Datum/Start/Ende/Kapazität) genug Platz haben. Grid-Layout reagiert jetzt responsiv: 1 Spalte ≤ sm, 2 Spalten ≥ sm, 4 Spalten mit gewichteter Aufteilung ≥ lg.

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
