# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

Monorepo with two independent applications:

- `frontend/` — Angular 21 application (scaffolded via `@angular/cli@21`, standalone components, Vitest test runner).
- `backend/` — Spring Boot 4.0.5 application on Java 21, Maven build (`spring-boot-starter-webmvc`).

There is no shared tooling (no root `package.json`, no Gradle multi-project). Each folder is built, tested, and run on its own.

## Frontend (`frontend/`)

Install once: `npm install` (scaffold was created with `--skip-install`).

Common commands (run inside `frontend/`):

- `npm start` — dev server on http://localhost:4200 (`ng serve`).
- `npm run build` — production build (`ng build`).
- `npm run watch` — incremental development build.
- `npm test` — Vitest test run (configured via `@angular/build`).

Notes:

- Entry points: `src/main.ts` bootstraps `src/app/app.ts` with config from `src/app/app.config.ts` and routes from `src/app/app.routes.ts`.
- Prettier config is inlined in `package.json` (100 cols, single quotes, Angular HTML parser).

## Backend (`backend/`)

Common commands (run inside `backend/`, the Maven wrapper is checked in):

- `./mvnw spring-boot:run` — start the application.
- `./mvnw test` — run tests.
- `./mvnw -Dtest=ClassName#method test` — run a single test.
- `./mvnw package` — build the runnable jar into `target/`.

Notes:

- Base package: `de.tellerstatttonne.backend`. Main class: `BackendApplication`.
- Currently only `spring-boot-starter-webmvc` (+ its test starter) is on the classpath — add further starters to `pom.xml` as features grow.

## Conventions

- Keep frontend and backend decoupled; don't introduce shared tooling at the repo root without a clear reason.
- Project name "Teller statt Tonne" (German: "plate instead of bin") suggests a food-waste / food-sharing domain, but product scope is not yet defined — ask the user before assuming requirements.
- Versioning follows SemVer per app. Track changes under `## [Unreleased]` in `backend/CHANGELOG.md` or `frontend/CHANGELOG.md` (Keep a Changelog format); on release move them into a `## [x.y.z] - YYYY-MM-DD` section and bump the version in `backend/pom.xml` / `frontend/package.json`.
