# 02 - Correcciones de contratos: courses, classes y ratings

Actualizaciones a los contratos definidos en `00_contracts.md`, surgidas durante la
integracion del sistema de ratings en el Frontend.

## 1. response_model tipado para courses y classes

`GET /courses`, `GET /courses/{slug}` y `GET /classes/{class_id}` antes devolvian
`list`/`dict` sin tipar (Swagger los documentaba como `object`/`array` con
`additionalProperties: true`, sin validacion real del contrato). Ahora usan
`response_model` con los schemas de `app/schemas/course.py`.

### GET /courses -> List[CourseResponse]

```json
[
  {
    "id": 1,
    "name": "Curso de React",
    "description": "Aprende React desde cero hasta convertirte en un desarrollador profesional",
    "thumbnail": "https://placehold.co/300x200?text=React+Course",
    "slug": "curso-de-react",
    "average_rating": 4.0,
    "total_ratings": 1
  }
]
```

Orden: los cursos se devuelven ordenados por `id` ascendente. Antes no tenian
`ORDER BY`, por lo que Postgres podia devolver primero registros de prueba
("Test Course") generados por los tests de integracion, desplazando a los cursos
reales del inicio del listado en la home.

### GET /courses/{slug} -> CourseDetailResponse

```json
{
  "id": 1,
  "name": "Curso de React",
  "description": "Aprende React desde cero hasta convertirte en un desarrollador profesional",
  "thumbnail": "https://placehold.co/300x200?text=React+Course",
  "slug": "curso-de-react",
  "teacher_id": [1, 2],
  "classes": [
    {
      "id": 1,
      "name": "Introducción a React",
      "description": "Conceptos básicos de React y JSX",
      "slug": "introduccion-a-react"
    }
  ],
  "average_rating": 4.0,
  "total_ratings": 1,
  "rating_distribution": {"1": 0, "2": 0, "3": 0, "4": 1, "5": 0}
}
```

> `classes[]` usa `CourseClassSummary` (`id`, `name`, `description`, `slug`).
> NO incluye `title`, `video` ni `duration` — esos campos solo existen en el detalle
> de una clase individual (`GET /classes/{class_id}`). El Frontend usa un tipo
> `ClassSummary` distinto de `Class` para reflejar esta diferencia.

### GET /classes/{class_id} -> ClassDetailResponse

```json
{
  "id": 1,
  "title": "Introducción a React",
  "description": "Conceptos básicos de React y JSX",
  "slug": "introduccion-a-react",
  "video": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "duration": 0
}
```

> `duration` esta hardcodeado a `0` (TODO pendiente en `main.py`): el modelo `Lesson`
> no almacena duracion real todavia.

## 2. GET /courses/{course_id}/ratings/user/{user_id}: respuesta 204

Cuando el usuario no ha calificado el curso, el endpoint responde `204 No Content`
(sin body, sin `content-type`), no `404`. Los clientes HTTP deben verificar
`response.status === 204` y tratarlo como "sin rating todavia", no como error.

`response_model=RatingResponse | None` documenta tambien la respuesta `200` con
`RatingResponse` cuando si existe un rating activo del usuario.

## 3. Datos de seed: thumbnails

`via.placeholder.com` dejo de estar disponible y rompia las imagenes de los cursos
en la home. Los 3 cursos de seed (React, Python, JavaScript) ahora usan
`https://placehold.co/300x200?text=...`, que soporta el mismo query param `?text=`
para el texto superpuesto.

## 4. Aislamiento de tests: test_rating_db_constraints.py

La fixture `sample_course` creaba un `Course` con slug unico por timestamp
(`test-course-{timestamp}`) y nunca lo eliminaba, acumulando registros "Test Course"
en la base de datos en cada corrida de `pytest` (se detectaron 21 registros
residuales con sus ratings asociados). Ahora la fixture hace `yield` y, al finalizar,
hace `rollback()` (para limpiar transacciones fallidas por los tests de CHECK
constraints) y elimina el curso junto con sus ratings.
