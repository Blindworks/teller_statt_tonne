# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository status

This repository is currently a stub. As of the latest commit, it contains only a `README.md` with the project title ("Teller statt Tonne" — German for "Plate instead of Bin"). There is no source code, build tooling, dependency manifest, test suite, CI configuration, or documented architecture yet.

As a result, there are no project-specific build, lint, test, or run commands to document. Update this file once a language/framework is chosen and initial scaffolding lands (e.g. `package.json`, `pyproject.toml`, `Cargo.toml`, `go.mod`, etc.).

## When extending this repo

- Before assuming a stack, check what files exist — the project may have been scaffolded since this note was written.
- The project name suggests a food-waste / food-sharing domain ("plate instead of bin"), but no requirements or architecture decisions have been committed. Do not infer product scope; ask the user.
- When adding the first real code, replace this section with concrete guidance: the chosen stack, entry points, the commands needed to build/test/run, and the high-level module boundaries that span multiple files.
