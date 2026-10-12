# PokeBallast Web (React)

The frontend of [PokeBallast](../README.md). The architecture and design decisions are in the main README.

## Requirements

* Node.js 22+ (developed with 24.21.0 LTS).
* The API on `http://localhost:8080`. Vite proxies `/api` to it.

## Commands

```bash
npm ci              # exact versions from package-lock.json
npm run dev         # http://localhost:5173
npm test            # Vitest + Testing Library + MSW
npm run lint        # ESLint (typescript-eslint, react-hooks, no-console)
npm run build       # type-check + production bundle (dist/)
docker build -t pokeballast-web .   # nginx image; proxies /api to http://api:8080 (see ../docker-compose.yml)
```

## Pages

| Route | Story | Access |
|---|---|---|
| `/` | US01: paginated list (sprite, category, mass, skills) | public |
| `/pokemon/:idOrName` | US02: detail, stats, description, evolution tree; US03 "Save to collection" | public; saving needs TRAINER/ADMIN |
| `/collection` | US03: list with name and tag filters, import, re-sync; admin delete | public read; actions by role |
| `/collection/:id/edit` | US04: localized name, region, tags | TRAINER/ADMIN |
| `/login`, `/register` | F05 | public |

## Notes

* Every server error is an `ApiError` carrying the backend code. The UI reacts per code: field errors (1000/1004/1007), conflicts (3001–3004), session expiry (4001) and PokeAPI outage (5001).
* If the project lives on a Windows drive under WSL (`/mnt/c`), file watching needs polling. It is already enabled in `vite.config.ts`.
