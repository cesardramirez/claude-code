'use client';

import { useState, useEffect } from 'react';
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

  useEffect(() => {
    ratingsApi
      .getUserRating(courseId, userId)
      .then((existing) => {
        if (existing) setUserRating(existing.rating);
      })
      .catch(() => {});
  }, [courseId, userId]);

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

  const displayRating = hoveredRating || userRating;

  return (
    <div className={styles.ratingSection}>
      <h3 className={styles.title}>Califica este curso</h3>

      <div className={styles.starsRow}>
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            className={`${styles.starBtn} ${displayRating >= star ? styles.active : ''}`}
            onClick={() => handleRating(star)}
            onMouseEnter={() => setHoveredRating(star)}
            onMouseLeave={() => setHoveredRating(0)}
            disabled={state === 'loading'}
            aria-label={`Calificar con ${star} estrella${star !== 1 ? 's' : ''}`}
            aria-pressed={userRating === star}
          >
            ★
          </button>
        ))}
        {userRating > 0 && (
          <span className={styles.selectedLabel}>
            {hoveredRating > 0 ? hoveredRating : userRating} / 5
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
