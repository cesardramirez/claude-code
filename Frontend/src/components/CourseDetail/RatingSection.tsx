'use client';

import { useState, useEffect, useRef } from 'react';
import { ratingsApi, ApiError } from '@/services/ratingsApi';
import { StarRating } from '@/components/StarRating/StarRating';
import type { RatingState } from '@/types/rating';
import styles from './RatingSection.module.scss';

interface RatingSectionProps {
  courseId: number;
  userId: number;
  initialAverageRating?: number;
  initialTotalRatings?: number;
}

export const RatingSection = ({
  courseId,
  userId,
  initialAverageRating = 0,
  initialTotalRatings = 0,
}: RatingSectionProps) => {
  const [userRating, setUserRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [averageRating, setAverageRating] = useState(initialAverageRating);
  const [totalRatings, setTotalRatings] = useState(initialTotalRatings);
  const [state, setState] = useState<RatingState>('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const starButtonRefs = useRef<(HTMLButtonElement | null)[]>([]);

  useEffect(() => {
    let isMounted = true;

    ratingsApi
      .getUserRating(courseId, userId)
      .then((existing) => {
        if (isMounted && existing) setUserRating(existing.rating);
      })
      .catch(() => {});

    return () => {
      isMounted = false;
    };
  }, [courseId, userId]);

  // Limpia el mensaje de error automáticamente tras un tiempo
  useEffect(() => {
    if (state !== 'error') return;

    const timeoutId = setTimeout(() => {
      setState('idle');
      setErrorMessage('');
    }, 4000);

    return () => clearTimeout(timeoutId);
  }, [state]);

  const handleRating = async (rating: number) => {
    const previousRating = userRating;
    setUserRating(rating);
    setState('loading');
    setErrorMessage('');

    try {
      await ratingsApi.createRating(courseId, { user_id: userId, rating });
      const stats = await ratingsApi.getRatingStats(courseId);
      setAverageRating(stats.average_rating);
      setTotalRatings(stats.total_ratings);
      setState('success');
      setTimeout(() => setState('idle'), 2000);
    } catch (error) {
      setUserRating(previousRating);
      setState('error');
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : 'Error al guardar tu calificación'
      );
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>, star: number) => {
    let nextStar: number | null = null;

    switch (event.key) {
      case 'ArrowRight':
      case 'ArrowUp':
        nextStar = Math.min(5, star + 1);
        break;
      case 'ArrowLeft':
      case 'ArrowDown':
        nextStar = Math.max(1, star - 1);
        break;
      default:
        return;
    }

    event.preventDefault();

    const nextButton = starButtonRefs.current[nextStar - 1];
    nextButton?.focus();
  };

  const displayRating = hoveredRating || userRating;

  return (
    <div className={styles.ratingSection}>
      <h3 className={styles.title}>Califica este curso</h3>

      <div className={styles.starsRow}>
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            ref={(el) => {
              starButtonRefs.current[star - 1] = el;
            }}
            className={`${styles.starBtn} ${displayRating >= star ? styles.active : ''}`}
            onClick={() => handleRating(star)}
            onMouseEnter={() => setHoveredRating(star)}
            onMouseLeave={() => setHoveredRating(0)}
            onFocus={() => setHoveredRating(star)}
            onBlur={() => setHoveredRating(0)}
            onKeyDown={(event) => handleKeyDown(event, star)}
            disabled={state === 'loading'}
            aria-label={`Calificar con ${star} estrella${star !== 1 ? 's' : ''}`}
            aria-pressed={userRating === star}
          >
            ★
          </button>
        ))}
        {displayRating > 0 && (
          <span className={styles.selectedLabel}>
            {displayRating} / 5
          </span>
        )}
      </div>

      {state === 'loading' && <p className={styles.feedback}>Guardando...</p>}
      {state === 'success' && (
        <p className={`${styles.feedback} ${styles.success}`}>
          ¡Calificación guardada!
        </p>
      )}
      {state === 'error' && (
        <p className={`${styles.feedback} ${styles.error}`}>{errorMessage}</p>
      )}

      {totalRatings > 0 && (
        <div className={styles.average}>
          <StarRating
            rating={averageRating}
            totalRatings={totalRatings}
            showCount
            size="medium"
          />
          <span className={styles.averageLabel}>
            {averageRating.toFixed(1)} promedio
          </span>
        </div>
      )}
    </div>
  );
};
