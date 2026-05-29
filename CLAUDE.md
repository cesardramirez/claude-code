# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Platziflix

Plataforma de cursos online multi-plataforma: API REST (FastAPI + PostgreSQL), web (Next.js 15), Android (Kotlin) e iOS (Swift).

## Arquitectura del Sistema

- **Backend**: API REST con FastAPI + PostgreSQL
- **Frontend**: Aplicación web con Next.js 15
- **Mobile**: Apps nativas Android (Kotlin) + iOS (Swift)

## Stack Tecnológico

### Backend (FastAPI/Python)
- **Framework**: FastAPI
- **Base de datos**: PostgreSQL 15
- **ORM**: SQLAlchemy 2.0
- **Migraciones**: Alembic
- **Container**: Docker + Docker Compose
- **Gestión dependencias**: UV
- **Puerto**: 8000

### Frontend (Next.js)
- **Framework**: Next.js 15 (App Router)
- **React**: 19.0
- **Lenguaje**: TypeScript
- **Estilos**: SCSS + CSS Modules
- **Testing**: Vitest + React Testing Library
- **Fonts**: Geist Sans & Geist Mono

### Mobile
- **Android**: Kotlin + Jetpack Compose + Retrofit
- **iOS**: Swift + SwiftUI + Repository Pattern

## Estructura del Proyecto

```
claude-code/
├── Backend/
│   ├── app/
│   │   ├── main.py
│   │   ├── alembic/versions/
│   │   ├── models/
│   │   ├── schemas/
│   │   ├── services/
│   │   ├── db/
│   │   ├── core/
│   │   └── tests/
│   ├── docker-compose.yml
│   ├── Dockerfile
│   └── Makefile
├── Frontend/
│   └── src/
│       ├── app/           # Next.js App Router
│       ├── components/
│       ├── services/
│       ├── types/
│       └── styles/
└── Mobile/
    ├── PlatziFlixAndroid/
    └── PlatziFlixiOS/
```

## Modelo de Datos

### Entidades Principales
- **Course**: Cursos (name, description, thumbnail, slug)
- **Teacher**: Profesores
- **Lesson**: Lecciones de un curso
- **Class**: Clases individuales de una lección
- **CourseRating**: Calificaciones de cursos (unique por course + user)

### Relaciones
- Course ↔ Teacher (Many-to-Many via course_teachers)
- Course → Lesson (One-to-Many)
- Lesson → Class (One-to-Many)

## API Endpoints

- `GET /` — Bienvenida
- `GET /health` — Health check + DB connectivity
- `GET /courses` — Lista todos los cursos
- `GET /courses/{slug}` — Detalle de curso por slug
- `GET /classes/{class_id}` — Detalle de clase
- `POST /courses/{id}/ratings` — Crear o actualizar rating (upsert)
- `GET /courses/{id}/ratings/stats` — Estadísticas de ratings
- `PUT /courses/{id}/ratings/{user_id}` — Actualizar rating
- `DELETE /courses/{id}/ratings/{user_id}` — Eliminar rating

## Comandos de Desarrollo

### Backend

El backend corre **exclusivamente dentro de Docker**. Nunca ejecutar comandos de Python o Alembic directamente en el host.

Todos los comandos se ejecutan desde `Backend/` usando el Makefile:

```bash
make start            # Levantar contenedores (docker-compose up -d)
make stop             # Detener contenedores
make restart          # Reiniciar contenedores
make build            # Construir imágenes
make migrate          # Aplicar migraciones Alembic
make seed             # Cargar datos de prueba
make seed-fresh       # Limpiar y recargar datos de prueba
make logs             # Ver logs en tiempo real
make clean            # Eliminar contenedores, volúmenes e imágenes
```

**Orden obligatorio al iniciar por primera vez:** `make start` → `make migrate` → `make seed`

Para crear una migración (evitar `make create-migration` ya que es interactivo):

```bash
docker-compose exec api bash -c "cd /app && uv run alembic -c app/alembic.ini revision --autogenerate -m 'descripcion'"
```

Para correr los tests del backend (no hay target en el Makefile):

```bash
docker-compose exec api bash -c "uv run pytest app/tests/"
```

El gestor de dependencias es **UV** (no pip). No usar `pip install`.

### Frontend

Usar siempre **yarn**, no npm:

```bash
cd Frontend
yarn dev    # Dev server con Turbopack
yarn test   # Vitest + React Testing Library
yarn lint   # ESLint
yarn build  # Build de producción
```

- Path alias `@/` apunta a `src/` — usarlo en todos los imports internos.
- TypeScript en modo strict.
- `vars.scss` se importa automáticamente en todos los archivos SCSS vía `next.config.ts` — no importarlo manualmente.

## URLs del Sistema

- **Backend API**: http://localhost:8000
- **Frontend Web**: http://localhost:3000
- **API Docs**: http://localhost:8000/docs (FastAPI Swagger)

## Base de Datos

### Configuración Docker
- **Usuario**: platziflix_user
- **Password**: platziflix_password
- **Database**: platziflix_db
- **Puerto**: 5432

### Migraciones
- Ubicación: `Backend/app/alembic/versions/`
- Comando crear: ver sección de comandos arriba
- Comando aplicar: `make migrate`

## Funcionalidades Implementadas

- ✅ Catálogo de cursos con grid estilo Netflix
- ✅ Detalle de cursos (profesores, lecciones, clases)
- ✅ Navegación por slug SEO-friendly
- ✅ Reproductor de video integrado
- ✅ Health checks de API y DB
- ✅ Apps móviles nativas (Android + iOS)
- ✅ Testing en todos los componentes
- ✅ Sistema de ratings de cursos (upsert)

## Patrones de Desarrollo

### Backend
- **Arquitectura**: Service Layer Pattern
- **Dependency Injection**: FastAPI Dependencies
- **Database**: Repository Pattern con SQLAlchemy
- **Soft deletes**: Los modelos usan `deleted_at` (no eliminación física)
- **Ratings**: `POST /courses/{id}/ratings` hace upsert — unique constraint en `(course_id, user_id)`

### Frontend
- **Routing**: Next.js App Router
- **Data Fetching**: Server Components + fetch
- **Styling**: CSS Modules + SCSS
- **Testing**: Component testing con Vitest

### Mobile
- **Android**: MVVM + Jetpack Compose
- **iOS**: SwiftUI + Repository + Mapper Pattern

## Consideraciones de Desarrollo

1. **Docker obligatorio** para el backend (DB + API)
2. **TypeScript strict** en Frontend
3. **Testing requerido** para nuevas funcionalidades
4. **Migraciones automáticas** para cambios de DB
5. **Convenciones de naming**: snake_case (Python), camelCase (JS/TS), PascalCase (Swift/Kotlin)
6. **API REST** como única fuente de datos para Frontend/Mobile
7. **Conventional Commits**: usar prefijos `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`

## Agentes Especializados

Hay agentes pre-configurados en `.claude/agents/`:
- `architect` — arquitectura, diseño de sistemas, contratos de API
- `backend` — FastAPI, Python, SQLAlchemy, PostgreSQL, pytest
- `frontend` — Next.js, React, TypeScript, SCSS, Vitest
