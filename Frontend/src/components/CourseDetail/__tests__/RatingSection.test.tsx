import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { RatingSection } from '../RatingSection';

vi.mock('@/services/ratingsApi', () => ({
  ratingsApi: {
    getUserRating: vi.fn(),
    createRating: vi.fn(),
    getRatingStats: vi.fn(),
  },
  ApiError: class ApiError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.name = 'ApiError';
      this.status = status;
    }
  },
}));

import { ratingsApi } from '@/services/ratingsApi';

const mockedApi = vi.mocked(ratingsApi);

const defaultProps = {
  courseId: 1,
  userId: 1,
};

describe('RatingSection', () => {
  beforeEach(() => {
    mockedApi.getUserRating.mockResolvedValue(null);
    mockedApi.createRating.mockResolvedValue({
      id: 1,
      course_id: 1,
      user_id: 1,
      rating: 4,
      created_at: '2026-01-01T00:00:00Z',
      updated_at: '2026-01-01T00:00:00Z',
    });
    mockedApi.getRatingStats.mockResolvedValue({
      average_rating: 4.0,
      total_ratings: 10,
    });
  });

  describe('Renderizado', () => {
    it('muestra el título', async () => {
      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });
      expect(screen.getByText('Califica este curso')).toBeInTheDocument();
    });

    it('renderiza 5 botones de estrella', async () => {
      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });
      const buttons = screen.getAllByRole('button');
      expect(buttons).toHaveLength(5);
    });

    it('muestra el promedio cuando hay ratings iniciales', async () => {
      await act(async () => {
        render(
          <RatingSection
            {...defaultProps}
            initialAverageRating={4.2}
            initialTotalRatings={25}
          />
        );
      });
      expect(screen.getByText('4.2 promedio')).toBeInTheDocument();
    });

    it('no muestra el promedio cuando totalRatings es 0', async () => {
      await act(async () => {
        render(
          <RatingSection
            {...defaultProps}
            initialAverageRating={0}
            initialTotalRatings={0}
          />
        );
      });
      expect(screen.queryByText(/promedio/)).not.toBeInTheDocument();
    });

    it('cada botón tiene aria-label descriptivo', async () => {
      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });
      expect(
        screen.getByRole('button', { name: 'Calificar con 1 estrella' })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Calificar con 3 estrellas' })
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: 'Calificar con 5 estrellas' })
      ).toBeInTheDocument();
    });
  });

  describe('Estado inicial del usuario', () => {
    it('carga el rating existente del usuario al montar', async () => {
      mockedApi.getUserRating.mockResolvedValue({
        id: 5,
        course_id: 1,
        user_id: 1,
        rating: 3,
        created_at: '2026-01-01T00:00:00Z',
        updated_at: '2026-01-01T00:00:00Z',
      });

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      expect(mockedApi.getUserRating).toHaveBeenCalledWith(1, 1);
      // El label del rating seleccionado debe estar visible
      expect(screen.getByText('3 / 5')).toBeInTheDocument();
    });

    it('no muestra selección si el usuario no ha calificado', async () => {
      mockedApi.getUserRating.mockResolvedValue(null);

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      expect(screen.queryByText(/\/ 5/)).not.toBeInTheDocument();
    });

    it('no falla si getUserRating lanza un error', async () => {
      mockedApi.getUserRating.mockRejectedValue(new Error('Network error'));

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      expect(screen.getByText('Califica este curso')).toBeInTheDocument();
    });
  });

  describe('Interacción — calificar', () => {
    it('llama a createRating al hacer click en una estrella', async () => {
      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      const btn4 = screen.getByRole('button', {
        name: 'Calificar con 4 estrellas',
      });

      await act(async () => {
        fireEvent.click(btn4);
      });

      expect(mockedApi.createRating).toHaveBeenCalledWith(1, {
        user_id: 1,
        rating: 4,
      });
    });

    it('muestra el mensaje de guardado y luego éxito', async () => {
      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      fireEvent.click(
        screen.getByRole('button', { name: 'Calificar con 5 estrellas' })
      );

      expect(screen.getByText('Guardando...')).toBeInTheDocument();

      await waitFor(() => {
        expect(screen.getByText('¡Calificación guardada!')).toBeInTheDocument();
      });
    });

    it('actualiza el promedio después de calificar', async () => {
      mockedApi.getRatingStats.mockResolvedValue({
        average_rating: 4.5,
        total_ratings: 11,
      });

      await act(async () => {
        render(
          <RatingSection
            {...defaultProps}
            initialAverageRating={4.0}
            initialTotalRatings={10}
          />
        );
      });

      await act(async () => {
        fireEvent.click(
          screen.getByRole('button', { name: 'Calificar con 5 estrellas' })
        );
      });

      await waitFor(() => {
        expect(screen.getByText('4.5 promedio')).toBeInTheDocument();
      });
    });

    it('deshabilita los botones mientras guarda', async () => {
      // createRating que nunca resuelve para mantener el estado loading
      mockedApi.createRating.mockReturnValue(new Promise(() => {}));

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      fireEvent.click(
        screen.getByRole('button', { name: 'Calificar con 3 estrellas' })
      );

      const buttons = screen.getAllByRole('button');
      buttons.forEach((btn) => {
        expect(btn).toBeDisabled();
      });
    });
  });

  describe('Manejo de errores', () => {
    it('muestra mensaje de error si createRating falla', async () => {
      mockedApi.createRating.mockRejectedValue(new Error('Server error'));

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      await act(async () => {
        fireEvent.click(
          screen.getByRole('button', { name: 'Calificar con 2 estrellas' })
        );
      });

      await waitFor(() => {
        expect(
          screen.getByText('Error al guardar tu calificación')
        ).toBeInTheDocument();
      });
    });

    it('revierte el rating optimista al fallar', async () => {
      mockedApi.getUserRating.mockResolvedValue({
        id: 1,
        course_id: 1,
        user_id: 1,
        rating: 3,
        created_at: '2026-01-01T00:00:00Z',
        updated_at: '2026-01-01T00:00:00Z',
      });
      mockedApi.createRating.mockRejectedValue(new Error('Error'));

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      // Verificar que el rating previo (3) está cargado
      await waitFor(() => {
        expect(screen.getByText('3 / 5')).toBeInTheDocument();
      });

      // Intentar cambiar a 5 — falla
      await act(async () => {
        fireEvent.click(
          screen.getByRole('button', { name: 'Calificar con 5 estrellas' })
        );
      });

      // Debe volver al rating anterior (3)
      await waitFor(() => {
        expect(screen.getByText('3 / 5')).toBeInTheDocument();
      });
    });
  });

  describe('Hover', () => {
    it('actualiza el label al hacer hover', async () => {
      mockedApi.getUserRating.mockResolvedValue({
        id: 1,
        course_id: 1,
        user_id: 1,
        rating: 2,
        created_at: '2026-01-01T00:00:00Z',
        updated_at: '2026-01-01T00:00:00Z',
      });

      await act(async () => {
        render(<RatingSection {...defaultProps} />);
      });

      await waitFor(() => {
        expect(screen.getByText('2 / 5')).toBeInTheDocument();
      });

      fireEvent.mouseEnter(
        screen.getByRole('button', { name: 'Calificar con 4 estrellas' })
      );

      expect(screen.getByText('4 / 5')).toBeInTheDocument();

      fireEvent.mouseLeave(
        screen.getByRole('button', { name: 'Calificar con 4 estrellas' })
      );

      expect(screen.getByText('2 / 5')).toBeInTheDocument();
    });
  });
});
