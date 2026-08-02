# AGENTS.md

Guidance for AI agents working in this repository.

## Repository status

This repository (`udaymengane111/Test`) is a minimal starter repo. It currently contains only `README.md` with no application source code, package manifests, Docker configuration, or CI workflows.

There are **no services to start**, **no lint/test scripts**, and **no build commands** until application code is added.

## Cursor Cloud specific instructions

### Environment overview

| Aspect | Status |
|--------|--------|
| Application code | Not present |
| Package manager / lockfile | None |
| Services to run | None |
| Environment variables | None required |
| Docker / compose | Not configured |

### VM tooling available

The Cloud Agent VM includes common development tools (Node.js via nvm, Python 3.12, git, make). These are preinstalled on the VM; no repo-specific dependency install is needed until a project stack is added.

### Startup / run

There is nothing to build or run in this repository yet. After adding an application (e.g. `package.json`, `requirements.txt`, or `docker-compose.yml`), document the standard dev commands here and in the README.

### Lint / test

No lint or test commands exist until a project toolchain is added. Once added, prefer the scripts defined in the project's manifest (e.g. `npm run lint`, `npm test`).

### Gotchas

- Do not assume a framework or language; inspect the repo for manifests before installing dependencies.
- The update script is a no-op (`true`) because there are no dependencies to refresh on startup.
