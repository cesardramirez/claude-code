import { test, expect } from '@playwright/test';

const API_URL = 'http://localhost:8000';
const COURSE_SLUG = 'curso-de-react';
const COURSE_ID = 1;
const USER_ID = 1;

test.describe('Sistema de Ratings', () => {
  test.beforeEach(async ({ request }) => {
    await request.delete(`${API_URL}/courses/${COURSE_ID}/ratings/${USER_ID}`);
  });

  test('muestra el RatingSection con 5 estrellas', async ({ page }) => {
    await page.goto(`/course/${COURSE_SLUG}`);

    await expect(page.getByText('Califica este curso')).toBeVisible();

    for (let i = 1; i <= 5; i++) {
      const label = `Calificar con ${i} estrella${i !== 1 ? 's' : ''}`;
      await expect(page.getByRole('button', { name: label })).toBeVisible();
    }
  });

  test('hover ilumina las estrellas y muestra el label', async ({ page }) => {
    await page.goto(`/course/${COURSE_SLUG}`);

    await page.getByRole('button', { name: 'Calificar con 3 estrellas' }).hover();
    await expect(page.getByText('3 / 5')).toBeVisible();

    await page.getByRole('button', { name: 'Calificar con 5 estrellas' }).hover();
    await expect(page.getByText('5 / 5')).toBeVisible();
  });

  test('guarda un rating y muestra mensaje de éxito', async ({ page }) => {
    await page.goto(`/course/${COURSE_SLUG}`);

    await page.getByRole('button', { name: 'Calificar con 4 estrellas' }).click();

    await expect(page.getByText('¡Calificación guardada!')).toBeVisible();
    await expect(page.getByText('4 / 5')).toBeVisible();
  });

  test('muestra el promedio actualizado tras calificar', async ({ page }) => {
    await page.goto(`/course/${COURSE_SLUG}`);

    await page.getByRole('button', { name: 'Calificar con 5 estrellas' }).click();

    await expect(page.getByText('¡Calificación guardada!')).toBeVisible();
    await expect(page.getByText('5.0 promedio')).toBeVisible();
  });

  test('persiste el rating al recargar la página', async ({ page }) => {
    await page.goto(`/course/${COURSE_SLUG}`);
    await page.getByRole('button', { name: 'Calificar con 3 estrellas' }).click();
    await expect(page.getByText('¡Calificación guardada!')).toBeVisible();

    await page.reload();
    await page.getByText('Califica este curso').scrollIntoViewIfNeeded();

    await expect(page.getByText('3 / 5')).toBeVisible();
  });

  test('actualiza un rating existente', async ({ page }) => {
    await page.goto(`/course/${COURSE_SLUG}`);

    await page.getByRole('button', { name: 'Calificar con 5 estrellas' }).click();
    await expect(page.getByText('¡Calificación guardada!')).toBeVisible();
    await expect(page.getByText('5.0 promedio')).toBeVisible();

    // Esperar estado idle antes de volver a calificar
    await page.waitForTimeout(2200);

    await page.getByRole('button', { name: 'Calificar con 2 estrellas' }).click();
    await expect(page.getByText('¡Calificación guardada!')).toBeVisible();
    await expect(page.getByText('2 / 5')).toBeVisible();
    await expect(page.getByText('2.0 promedio')).toBeVisible();
  });

  test('funciona en los tres cursos disponibles', async ({ page, request }) => {
    const cursos = [
      { slug: 'curso-de-react', id: 1 },
      { slug: 'curso-de-python', id: 2 },
      { slug: 'curso-de-javascript', id: 3 },
    ];

    for (const curso of cursos) {
      await request.delete(`${API_URL}/courses/${curso.id}/ratings/${USER_ID}`);

      await page.goto(`/course/${curso.slug}`);
      await page.getByRole('button', { name: 'Calificar con 4 estrellas' }).click();
      await expect(page.getByText('¡Calificación guardada!')).toBeVisible();
      await expect(page.getByText('4 / 5')).toBeVisible();
    }
  });
});
