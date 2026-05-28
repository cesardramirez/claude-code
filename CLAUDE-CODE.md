# Listado de Prompts

## Instalación de Docker
1. Ingresar a https://get.docker.com/
2. Ejecutar el script que se indica al inicio del archivo
```shell
cd Download && curl -fsSL https://get.docker.com -o install-docker.sh
sudo sh install-docker.sh
sudo usermod -aG docker [nombre_usuario_linux]  # Define que el usuario tenga permisos para ejecutar el demonio. User: desarrollo.
sudo shutdown -r 0
docker -v
docker version
```

## Instalación de Docker Compose

## Ejecutar Backend
```text
Ahora ayúdame a tener el servicio de @Backend/ corriendo en mi local, verás que ya tengo docker instalado y listo para correr.
```
Claude Code ejecutará los siguientes comandos:
```shell
# Levantar los contenedores en segundo plano.
cd .../Backend && docker compose up -d
# Ejecutar migraciones (creación de la BD y sus tablas)
docker compose exec api bash -c "cd /app && uv run alembic -c app/alembic.ini upgrade head"
# Ejecutar el seed (poblar la BD con datos de prueba)
docker compose exec api bash -c "cd /app && uv run python -m app.db.seed"
```
> Todo listo. El backend está corriendo correctamente.
> - Contenedores: PostgreSQL + API levantados.
> - Migraciones: aplicadas (schema inicial + tabla course_ratings).
> - Datos de prueba: 3 profesores, 3 cursos, 6 lecciones.
> 
> Puedes probar en:
> - http://localhost:8000 — API
> - http://localhost:8000/docs — Swagger UI
> - http://localhost:8000/courses — Lista de cursos
>
> Levantamos el backend de Platziflix con Docker: contenedores corriendo, migraciones aplicadas y datos de prueba cargados. El servicio está listo en localhost:8000. (disable recaps in /config)

### API
![API](docs/img/02_Localhost.png)

### Swagger UI
![Swagger UI](docs/img/03_Swagger_UI.png)

### Listado de Cursos
![Listado Cursos](docs/img/04_Listado_Cursos.png)