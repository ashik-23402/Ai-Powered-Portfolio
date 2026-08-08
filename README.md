# AskAboutMe

A retrieval-augmented Q&A system: upload documents about yourself (resume, project write-ups, profile notes), they get chunked and embedded into a vector store, and a `/ask` endpoint answers natural-language questions about them using Gemini, grounded only in what was uploaded.

The repo is a small monorepo with three parts:

| Part | What it is | Stack |
|---|---|---|
| [`src/`](src) | The backend API — file ingestion, embedding pipeline, and the `/ask` endpoint | Spring Boot 4, Java 25, Spring AI |
| [`Admin-Frontend/`](Admin-Frontend) | An internal console for uploading and managing source documents | Vanilla HTML/CSS/JS, no build step |
| [`Portfolio-web/`](Portfolio-web) | A public-facing portfolio site with an "Ask AI" chat widget wired to `/ask` | Vanilla HTML/CSS/JS, no build step |

## How it works

```
Admin-Frontend                              Portfolio-web
     │                                            │
     │ upload docs (Basic Auth)                   │ ask questions (public)
     ▼                                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Spring Boot backend                     │
│                                                           │
│  POST /api/v1/files/*  ──▶  MinIO (object storage)       │
│                          ──▶  Postgres (file metadata)    │
│                                                           │
│  every ~1 min: EmbeddingSchedulerService                 │
│    reads COMPLETED files ──▶ chunks + embeds (Ollama,    │
│    nomic-embed-text) ──▶ stores vectors in pgvector       │
│                                                           │
│  POST /api/v1/ask                                        │
│    question ──▶ pgvector similarity search (top K)       │
│              ──▶ Gemini (gemini-flash-latest), answers   │
│                  only from the retrieved context         │
└─────────────────────────────────────────────────────────┘
```

Deleting a file soft-deletes it immediately and kicks off best-effort cleanup of its storage object and vector rows; a scheduled retry job (`FileDeletionSchedulerService`) keeps trying until cleanup succeeds.

## Tech stack

- **Backend**: Java 25, Spring Boot 4.1, Spring Web MVC, Spring Security (HTTP Basic), Spring Data JPA, Spring AI (Google GenAI + Ollama + pgvector starters), ShedLock (scheduler locking), springdoc-openapi, Lombok
- **Storage**: PostgreSQL 16 + [pgvector](https://github.com/pgvector/pgvector) for embeddings, MinIO for raw file storage
- **Embeddings**: Ollama running `nomic-embed-text` locally
- **LLM**: Google Gemini (`gemini-flash-latest`) via Spring AI
- **Frontends**: no framework, no bundler — plain HTML/CSS/JS on both `Admin-Frontend` and `Portfolio-web`

## Repository structure

```
askaboutme/
├── src/main/java/com/ashik/askaboutme/
│   ├── controller/     AskController, FileUploadController
│   ├── service/        embedding pipeline, file storage, scheduled jobs
│   ├── config/          security, CORS, rate limiting, OpenAPI, MinIO, ShedLock
│   ├── configdto/       @ConfigurationProperties records
│   ├── ratelimit/       per-IP rate limiter + servlet filter
│   ├── dto/             request/response records
│   ├── entity/          FileUpload JPA entity + status enums
│   ├── repository/      Spring Data JPA repository
│   └── exception/       ErrorResponse + global exception handler
├── src/main/resources/application.yaml
├── compose.yaml          ollama, pgvector, minio
├── Admin-Frontend/       internal file-management console
└── Portfolio-web/        public portfolio site + AI chat widget
```

## Prerequisites

- Java 25 (the Gradle toolchain will fetch it if not already installed)
- Docker + Docker Compose (for Postgres/pgvector, MinIO, Ollama)
- A [Gemini API key](https://aistudio.google.com/apikey)
- Python 3 (or any static file server) if you want to serve the two frontends locally

## Getting started

### 1. Configure environment variables

```bash
cp .env.example .env
```

Fill in `.env` at the repo root:

| Variable | Purpose | Default (from `application.yaml`) |
|---|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | pgvector container + JDBC credentials | `myuser` / `secret` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO container credentials | `minioadmin` / `minioadmin` |
| `GEMINI_API_KEY` | Google Gemini API key used for chat completions | *(required, no default)* |
| `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD` | HTTP Basic credentials for the admin/file APIs | `admin` / `change-me` |

`.env` is gitignored and is read two ways: `compose.yaml` picks it up for the container credentials, and `build.gradle` loads it and injects it into `./gradlew bootRun` / `./gradlew test` so Spring's `${VAR}` placeholders resolve without hardcoding secrets in `application.yaml`.

### 2. Start the supporting services

```bash
docker compose up -d
```

This brings up:
- **pgvector** (`pgvector/pgvector:pg16`) on `5432` — file metadata + vector store
- **minio** on `9000` (API) / `9001` (console) — raw file storage
- **ollama** on `11434`, plus a one-shot `ollama-init` job that pulls the `nomic-embed-text` model on first run

> Spring Boot's Docker Compose integration will also auto-start these when you run the app if they aren't already up — `docker compose up -d` just makes startup faster and lets you inspect the containers directly.

### 3. Run the backend

```bash
./gradlew bootRun
```

The API comes up on `http://localhost:8080`. Interactive docs: `http://localhost:8080/swagger-ui.html` (raw spec at `/v3/api-docs`).

### 4. Serve the frontends

Each frontend is static — no build step — and reads its backend URL from a runtime `.env` file (same `KEY=VALUE` convention as the backend), fetched at load time and defaulting to `http://localhost:8080` if absent.

```bash
# Admin console (upload/manage source documents)
cp Admin-Frontend/.env.example Admin-Frontend/.env
cd Admin-Frontend && python3 -m http.server 8090
# → http://localhost:8090, log in with APP_SECURITY_USERNAME / APP_SECURITY_PASSWORD

# Portfolio site (public, with the Ask AI chat widget)
cp Portfolio-web/.env.example Portfolio-web/.env
cd Portfolio-web && python3 -m http.server 8091
# → http://localhost:8091
```

## API overview

Full request/response schemas are in Swagger UI. Summary:

### `POST /api/v1/ask` — public

Ask a question grounded in whatever's been uploaded and embedded so far.

```bash
curl -X POST http://localhost:8080/api/v1/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the AskAboutMe project for?"}'
```

Rate-limited per client IP (see below). No authentication required.

### `POST /api/v1/files/*` — HTTP Basic auth required

Chunked multipart upload lifecycle for source documents (PDF, DOCX, DOC, MD, TXT):

1. `POST /api/v1/files/initiate` — open an upload session, get back a `fileId`
2. `POST /api/v1/files/{fileId}/parts/{partNumber}` — upload each chunk (multipart form data)
3. `POST /api/v1/files/{fileId}/complete` — assemble the parts; the file is picked up by the embedding scheduler within about a minute
4. `GET /api/v1/files` — list completed files with short-lived preview URLs
5. `DELETE /api/v1/files/{fileId}` — soft-delete + best-effort cleanup of storage and vectors

## Rate limiting

`POST /api/v1/ask` is rate-limited per client IP using an in-memory fixed-window counter (`IpRateLimiter`) — no external dependency, since the app runs as a single instance. Defaults:

```yaml
app:
  rate-limit:
    ask:
      max-requests: 10
      window: 1m
```

Exceeding the limit returns `429 Too Many Requests` with the same `ErrorResponse` shape used everywhere else. Both frontends translate this into a plain-language message rather than showing the raw API error.

## Security notes

- Everything under `/api/v1/files/**` requires HTTP Basic auth (`APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD`); `/api/v1/ask` and the Swagger endpoints are public.
- CORS currently allows all origins (`allowedOriginPatterns: "*"`) — tighten this in `CorsConfig` before exposing the backend beyond local development.
- Never commit real values into `application.yaml`; secrets belong only in the gitignored `.env` files.

## License

No license file is currently included in this repository.
