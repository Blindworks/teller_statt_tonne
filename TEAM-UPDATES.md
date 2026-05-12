# Team-Updates

Kurze, alltagsnahe Zusammenfassung der Änderungen an „Teller statt Tonne" — gedacht zum Weiterleiten ans Team.
Für die technische Detailansicht siehe [`backend/CHANGELOG.md`](backend/CHANGELOG.md) und [`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

---

## 11.05.2026

### Neu

- **Veranstaltungen als kurzlebige Abholungsorte.** Neuer Menüpunkt „Veranstaltungen" für alle eingeloggten Rollen. Teamleitung und Admin legen Events mit Name, Beschreibung, Start-/Enddatum, Adresse, Ansprechperson und optionalem Logo an; vergangene und aktive Events sind über Tabs trennbar. Aktive Events erscheinen mit eigenem Marker auf der Karte. Pickups, die zu einem Event gehören, tauchen für **alle** Retter:innen auf dem Dashboard auf — unabhängig davon, welchem Betrieb sie zugeordnet sind. *Was heißt das fürs Team?* Sonderaktionen (Stadtfest, Tafel-Event, Spendenlauf) können jetzt als eigenständiger Abholungsort geführt werden, ohne dass wir dafür einen Fake-Betrieb anlegen müssen.

- **Teller-Treffs (Verteilerplätze) im Admin-Bereich.** Neue Verwaltung unter „Verteilerplätze" für Teamleitung und Admin: Name (Pflicht), Beschreibung, Adresse, Mehrfach-Auswahl von Betreiber:innen und beliebig viele Öffnungszeiten-Slots (Wochentag + Von/Bis). *Was heißt das fürs Team?* Endlich ein zentraler Ort, an dem wir festhalten, wo wir gerettetes Essen verteilen — wer den Ort betreut und wann er geöffnet ist.

- **Betriebs-Kategorien selbst verwalten.** Neue Admin-Seite „Betrieb-Kategorien": Kategorien anlegen, umbenennen, Icon aus einer kuratierten Lebensmittel-Auswahl wählen, Reihenfolge bestimmen und einzeln aktiv/inaktiv schalten. Inaktive Kategorien verschwinden aus Dropdowns und Filtern, bleiben aber an bestehenden Betrieben erhalten. *Was heißt das fürs Team?* Wenn wir z. B. „Hofladen" oder „Kantine" als neue Kategorie brauchen, richten wir die selbst ein — kein Entwickler nötig.

  > Hinweis: Eine Kategorie kann erst gelöscht werden, wenn kein Betrieb sie mehr verwendet. Falls das nicht der Fall ist, einfach auf „inaktiv" stellen.

- **Metzgerei als neue Betriebs-Kategorie.** Neben Bäckerei, Supermarkt, Café und Restaurant gibt es jetzt auch **Metzgerei** (Icon: Spieß). Auswählbar im Betrieb-Edit, in den Kartenfiltern und korrekt dargestellt in den Pickup-Karten. *Was heißt das fürs Team?* Metzgereien werden nicht mehr ersatzweise als „Supermarkt" geführt — Filter und Statistik werden dadurch sauberer.

---

## 08.05.2026

### Neu

- **Passwort vergessen?** Auf der Login-Seite gibt es jetzt unter dem Anmelden-Button den Link „Passwort vergessen?". Wer ihn anklickt, gibt seine E-Mail ein und bekommt — sofern die Adresse bei uns registriert ist — eine Mail mit einem Reset-Link. Der Link ist 30 Minuten gültig und führt auf eine Seite, auf der ein neues Passwort gesetzt werden kann. Aus Sicherheitsgründen werden nach erfolgreichem Reset alle bestehenden Anmeldungen abgemeldet. *Was heißt das fürs Team?* Niemand muss mehr beim Admin betteln, wenn er sein Passwort vergessen hat — der Self-Service erledigt das.

> Hinweis: Ob die Mail wirklich verschickt wird, hängt davon ab, dass auf dem Server SMTP-Zugangsdaten hinterlegt sind. Falls jemand einen Reset auslöst und keine Mail bekommt, kurz beim Entwicklerteam melden.

- **Logo-Upload für Betriebe.** Im Betrieb-Editor (✏️ Bearbeiten-Button auf einem Betrieb) lässt sich jetzt direkt eine Bilddatei als Logo hochladen — JPG, PNG, WebP oder GIF, bis 5 MB. Das bisherige URL-Feld bleibt parallel bestehen, falls man lieber einen Link einträgt. Hochgeladene Logos erscheinen sofort in der Betriebe-Liste, im Detail-Dialog, im Dashboard und in der Mitglieder-Verwaltung. *Was heißt das fürs Team?* Niemand muss mehr ein Logo erst irgendwo extern hosten — Datei auswählen, fertig.

> Hinweis: Bei einem **neuen** Betrieb erst einmal speichern, dann ist der Upload-Button aktiv.

- **Hygienezertifikat im Profil hochladen.** Im Profil gibt es einen neuen Bereich „Hygienezertifikat": User laden ihr Zertifikat (PDF, JPG, PNG oder WebP, bis 10 MB) zusammen mit dem Ausstellungsdatum hoch. Ein Status-Badge (Ausstehend / Genehmigt / Abgelehnt), ein Vorschau-Button und — bei Ablehnung — die Begründung sind direkt sichtbar. Wer noch kein Retter ist, sieht zusätzlich einen Hinweistext, dass das Zertifikat Voraussetzung für die Retter-Rolle ist. Re-Upload ersetzt das alte Dokument und setzt den Status zurück auf „Ausstehend". *Was heißt das fürs Team?* Niemand muss Zertifikate mehr per Mail rumschicken — und keine Sorge: andere Retter:innen können fremde Zertifikate nicht sehen.

- **Hygienezertifikate prüfen (Teamleitung & Admin).** Neue Seite „Hygienezertifikate" mit Status-Filter (Offen / Genehmigt / Abgelehnt / Alle), eingebetteter PDF- oder Bild-Vorschau, Genehmigen-Button und Inline-Begründungsfeld beim Ablehnen. Genehmigen vergibt automatisch die Retter-Rolle (und entfernt die „Neues Mitglied"-Rolle). *Was heißt das fürs Team?* Onboarding läuft in einem zentralen Workflow statt über verstreute Mails — und der Übergang vom „Neuen Mitglied" zum Retter ist nur einen Klick entfernt.

- **Klarer Lebenszyklus für Nutzer:innen.** Im User-Edit zeigt ein Status-Bereich, wo eine Person gerade steht: **Ausstehend** (Onboarding läuft), **Aktiv**, **Pausiert**, **Ausgetreten** oder **Entfernt**. Bei „Ausstehend" sieht Teamleitung & Admin den Fortschritt mit Häkchen für Einführungsgespräch und Hygienezertifikat. Status-Wechsel laufen über dedizierte Buttons („Einführung bestätigen", „Pausieren", „Reaktivieren", „Austreten", „Entfernen") — kein freies Status-Dropdown mehr. *Was heißt das fürs Team?* Wir sehen auf einen Blick, ob jemand wirklich „loslegen" kann, oder ob noch etwas im Onboarding fehlt.

- **Einladungs-Mail beim Anlegen eines Nutzers.** Beim Anlegen eines Users gibt es kein Passwort-Feld mehr — stattdessen bekommt die Person automatisch eine Mail mit Link zum eigenen Passwort-Setzen. Im User-Edit gibt es einen Button **„Einladung erneut senden"**, solange der User noch kein Passwort gesetzt hat. *Was heißt das fürs Team?* Niemand muss sich mehr ein Initialpasswort ausdenken und weitergeben — und wenn die erste Mail versehentlich gelöscht wird, ein Klick genügt.

  > Hinweis: Wie bei „Passwort vergessen?" hängt der tatsächliche Mail-Versand davon ab, dass die SMTP-Zugangsdaten auf dem Server hinterlegt sind.

- **Systemlog für Admins.** Neue Admin-Seite „Systemlog" zeigt schreibgeschützt alle wichtigen System-Ereignisse: Logins, Passwort-Resets, User-Anlage/-Löschung, Rollen-Änderungen, Hygienezertifikat-Entscheidungen, Betriebs-Aktionen, Bewerbungs-Entscheidungen, Mail-Versand-Fehler und nicht behandelte Server-Fehler. Filter für Kategorie, Event-Typ, Severity, Datums-Bereich und Volltextsuche über Meldung und Akteur-E-Mail. *Was heißt das fürs Team?* Wenn wir wissen wollen „wer hat wann wem die Rolle entzogen?" oder „warum kam diese Mail nicht an?", schauen wir hier nach.

  > Hinweis: Einträge älter als 90 Tage werden nachts automatisch gelöscht (Standardwert, anpassbar über `SYSTEMLOG_RETENTION_DAYS`).

---

## 07.05.2026

> 🚀 **Live ausgerollt am 07.05.2026** — die folgenden Änderungen sind in der Produktiv-App verfügbar.

### Neu

- **Erwartete Kilogramm pro Abholzeit.** Im Betrieb-Editor lässt sich pro Slot (z. B. Montag 18:30–19:00) eine erwartete Menge in Kilogramm hinterlegen — optional, leer ist erlaubt. Beim Anlegen einer neuen Abholung wird der Wert als Vorschlag für „Gerettet (kg)" übernommen und kann dort überschrieben werden. *Was heißt das fürs Team?* Wir haben endlich eine realistische Datenbasis für „Wieviel haben wir gerettet?", ohne dass jemand bei jeder Abholung von Hand wiegen müsste.
- **Statistik-Dashboard.** Neuer Menüpunkt „Statistik" (für Teamleitung & Admin) mit der Gesamtmenge geretteter Kilogramm, der Anzahl abgeschlossener Abholungen sowie den Top-10-Betrieben und Top-10-Retter:innen — jeweils sortiert nach Kilo. *Was heißt das fürs Team?* Auf einen Blick sehen, was die Aktion bewirkt — gut für Berichte, Förderanträge und Social-Media-Posts.
- **Automatische Notiz bei Statusänderung eines Betriebs.** Sobald sich der Kooperationsstatus ändert (z. B. von „Verhandlungen laufen" auf „Kooperiert"), entsteht automatisch eine interne Notiz im Verlauf des Betriebs — mit altem und neuem Status sowie der Person, die die Änderung vorgenommen hat. *Was heißt das fürs Team?* Wir können später nachvollziehen, wann ein Betrieb wie eingestuft war, ohne extra eine Notiz schreiben zu müssen.
- **Aus „Botschafter:in" wird „Teamleitung".** Die Rolle wurde durchgängig in der ganzen App umbenannt — in Texten, Listen, Profilen und im Datenschutz. Die Berechtigungen sind unverändert. *Was heißt das fürs Team?* Wer bisher Botschafter:in war, sieht ab jetzt überall „Teamleitung" — gleicher Zugriff, neuer Name.
- **Bewerbung auf Betriebe.** Retter:innen können sich auf der Betriebe-Seite mit einem Klick auf einen Betrieb bewerben (optional mit kurzer Nachricht). Auf der Karte erscheint dann „Bewerbung offen", bis Teamleitung oder Admin entschieden hat — bei bestehender Mitgliedschaft steht „Mein Betrieb". *Was heißt das fürs Team?* Neue Retter:innen müssen nicht mehr separat ansprechen, sondern starten den Prozess selbst.
- **Bewerbungs-Übersicht für Teamleitung & Admin.** Im Admin-Bereich gibt es eine neue Karte „Bewerbungen". Dort sind alle offenen Bewerbungen pro Betrieb gruppiert sichtbar — inklusive Adresse, damit unterschiedliche Filialen (z. B. zwei REWEs) klar auseinanderzuhalten sind. Annehmen ordnet die Person dem Betrieb zu, Ablehnen erlaubt eine kurze Begründung. *Was heißt das fürs Team?* Ein zentraler Ort statt verstreuter Mails — und die Person erhält automatisch eine Benachrichtigung über die Entscheidung.
- **„Meine Bewerbungen" für Retter:innen.** Eigene Bewerbungen mit Status (offen / angenommen / abgelehnt / zurückgezogen) und ggf. Begründung der Ablehnung. Offene Bewerbungen können selbst zurückgezogen werden. Verlinkt vom Dashboard, sobald jemand noch keinem Betrieb zugeordnet ist.
- **Neue Benachrichtigungen über die Glocke oben rechts.** Teamleitung & Admin werden bei jeder neuen Bewerbung benachrichtigt, Bewerber:innen bei der Entscheidung.
- **Filter in der Betriebe-Übersicht.** Über der Liste gibt es jetzt eine Suchleiste (filtert nach Name, Straße oder PLZ) sowie zwei Dropdowns für Kategorie (Bäckerei, Supermarkt, Café, Restaurant) und Kooperationsstatus. Filter lassen sich kombinieren, und ein „Filter zurücksetzen"-Button erscheint, sobald ein Filter aktiv ist. *Was heißt das fürs Team?* Bei vielen Betrieben muss niemand mehr scrollen, um z. B. „alle Bäckereien, mit denen wir noch verhandeln" zu finden.
- **„Interactive Map"-Kachel auf der Betriebe-Seite entfernt.** Die englische Promo-Kachel am Seitenende war redundant — die Karte ist weiterhin über den Sidebar-Eintrag „Karte" erreichbar.
- **Notizen pro Betrieb.** Im Betrieb-Editor gibt es jetzt direkt neben dem Hauptansprechpartner einen Notiz-Verlauf. Dort lassen sich Gesprächsinhalte, Absprachen und Beobachtungen festhalten — chronologisch, mit Autor und Datum. *Was heißt das fürs Team?* Wir müssen Gesprächsstand nicht mehr in Mails oder Chats suchen, sondern sehen ihn direkt am Betrieb.
- **Sichtbarkeit pro Notiz wählbar.** Beim Anlegen entscheiden Teamleitung und Admins, ob eine Notiz „Sichtbar für alle" (inklusive Retter) oder „Intern" (nur Teamleitung + Admins) ist. So können sensible Themen festgehalten werden, ohne dass sie alle sehen.
- **Notizen sind historisiert.** Einmal gespeicherte Notizen lassen sich nicht mehr bearbeiten — Korrekturen erfolgen über eine neue Notiz. Teamleitung und Admins können Notizen entfernen; sie bleiben aber für die Historie in der Datenbank erhalten.

### Verbessert

- **Einheitliche Bestätigungs-Dialoge.** Statt der grauen Browser-Popups („OK / Abbrechen") erscheinen jetzt überall App-eigene Dialoge im gewohnten Look — z. B. beim Löschen eines Nutzers, einer Notiz oder einer Quiz-Frage sowie beim Annehmen einer Bewerbung.

---

## 06.05.2026

### Neu

- **Admin-Bereich mit eigener Rollenverwaltung.** Administratoren können neue Rollen anlegen, umbenennen und festlegen, was eine Rolle darf — direkt in der App, ohne Hilfe vom Entwicklerteam. *Was heißt das fürs Team?* Wenn wir zukünftig z. B. eine Rolle „Praktikant:in" oder „Stützpunktleitung" brauchen, können wir die selbst einrichten.
- **Feiertage in der Pickup-Wochenansicht.** Bundesweite und NRW-spezifische Feiertage (z. B. Allerheiligen, Fronleichnam) werden in der Wochenübersicht markiert. So plant niemand mehr aus Versehen für einen Feiertag.
- **Impressum und Datenschutzerklärung sind live.** Beide Seiten sind über die Startseite verlinkt und enthalten die aktuellen Vorstandsdaten und den korrekten Vereinsregister-Status.

### Verbessert

- **Push-Benachrichtigungen kommen jetzt auch auf iPhones und im Safari-Browser an.** Vorher hat Apple unsere Test-Benachrichtigungen abgelehnt; das ist jetzt behoben. Android und Chrome waren nie betroffen.

### Im Hintergrund

- **Aktualisierte Deploy-Konfiguration.** Damit die neuen Push-Benachrichtigungen funktionieren, mussten zusätzliche Sicherheits-Schlüssel auf dem Server hinterlegt werden. Wer in Zukunft die App neu aufsetzt, findet die nötigen Schritte in der `DEPLOY.md`.

---

## 05.05.2026

Keine Änderungen an diesem Tag.
