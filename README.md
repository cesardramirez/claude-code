# claude-code
Platzi - Curso de Claude Code

## Index
- [Big Picture](#big-picture)

## Big Picture

![Big Picture](docs/img/01_Big_Picture.png)

## Backend - FastAPI + PostgreSQL
### 🧱 Arquitectura: Layered Architecture (Service Layer Pattern)
```text
📦 app
├── 📄 main.py            ← Endpoints REST (10 rutas).
├── 📄 core/config.py     ← Settings centralizados.
├── 📂 models/            ← SQLAlchemy ORM (entidades con soft deletes).
│   ├── 📄 course.py      ← Course (slug único, M2M teachers, 1-M lessons/ratings).
│   ├── 📄 teacher.py     ← Teacher (email único).
│   ├── 📄 lesson.py      ← Lesson (video_url, slug).
│   ├── 📄 course_rating.py ← Rating 1-5, unique(course_id, user_id).
├── 📂 schemas/           ← Pydantic DTOs (validación request/response).
├── 📂 services/          ← CourseService (toda la lógica de negocio).
├── 📂 db/                ← Engine SQLAlchemy + seed
└── 📂 alembic/           ← Migraciones de DB.
```

### 🎯 Endpoints disponibles

| Método | Ruta | Descripción |
| ------ | ---- | ----------- |
| GET | /courses | Lista cursos con rating stats |
