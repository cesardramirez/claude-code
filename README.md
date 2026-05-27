# claude-code
Platzi - Curso de Claude Code

## Index
- [Big Picture](#big-picture)

## Big Picture

![Big Picture](docs/img/01_Big_Picture.png)

## Backend - FastAPI + PostgreSQL
### 🧱 Arquitectura: Layered Architecture (Service Layer Pattern)
📦 app/<br>
├── 📄 main.py            ← Endpoints REST (10 rutas). <br>
├── 📄 core/config.py     ← Settings centralizados.<br>
├── 📂 models/            ← SQLAlchemy ORM (entidades con soft deletes).<br>
│   ├── 📄 course.py      ← Course (slug único, M2M teachers, 1-M lessons/ratings).<br>
│   ├── 📄 teacher.py     ← Teacher (email único).<br>
│   ├── 📄 lesson.py      ← Lesson (video_url, slug).<br>
│   └── 📄 course_rating.py ← Rating 1-5, unique(course_id, user_id).<br>
├── 📂 schemas/           ← Pydantic DTOs (validación request/response).<br>
├── 📂 services/          ← CourseService (toda la lógica de negocio).<br>
├── 📂 db/                ← Engine SQLAlchemy + seed.<br>
└── 📂 alembic/           ← Migraciones de DB.<br>

### 🎯 Endpoints disponibles

| Método | Ruta | Descripción |
| ------ | ---- | ----------- |
| GET | /courses | Lista cursos con rating stats |
