# Team-Updates

Kurze, alltagsnahe Zusammenfassung der Änderungen an „Teller statt Tonne" — gedacht zum Weiterleiten ans Team.
Für die technische Detailansicht siehe [`backend/CHANGELOG.md`](backend/CHANGELOG.md) und [`frontend/CHANGELOG.md`](frontend/CHANGELOG.md).

---

## 08.05.2026

### Neu

- **Logo-Upload für Betriebe.** Im Betrieb-Editor (✏️ Bearbeiten-Button auf einem Betrieb) lässt sich jetzt direkt eine Bilddatei als Logo hochladen — JPG, PNG, WebP oder GIF, bis 5 MB. Das bisherige URL-Feld bleibt parallel bestehen, falls man lieber einen Link einträgt. Hochgeladene Logos erscheinen sofort in der Betriebe-Liste, im Detail-Dialog, im Dashboard und in der Mitglieder-Verwaltung. *Was heißt das fürs Team?* Niemand muss mehr ein Logo erst irgendwo extern hosten — Datei auswählen, fertig.

> Hinweis: Bei einem **neuen** Betrieb erst einmal speichern, dann ist der Upload-Button aktiv.

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
