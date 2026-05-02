# Changelog

Dieses Repository ist ein Monorepo mit zwei eigenständigen Apps. Jede App pflegt ihren eigenen Changelog:

- **Backend**: [`backend/CHANGELOG.md`](backend/CHANGELOG.md)
- **Frontend**: [`frontend/CHANGELOG.md`](frontend/CHANGELOG.md)

Beide folgen [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/) und [Semantic Versioning](https://semver.org/lang/de/).

## Workflow

1. Während der Entwicklung Einträge unter `## [Unreleased]` im jeweiligen App-Changelog ergänzen (Kategorien: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`).
2. Beim Release: `Unreleased`-Sektion in eine neue `## [x.y.z] - YYYY-MM-DD`-Sektion umbenennen, Versionsnummer in `backend/pom.xml` bzw. `frontend/package.json` setzen, Tag (`backend-vX.Y.Z` / `frontend-vX.Y.Z`) anlegen.
