package com.espaciotiago.platziflixandroid.domain.models

import kotlinx.serialization.Serializable

/**
 * Domain model representing a Course.
 *
 * Marked as [Serializable] so it can be passed directly as a type-safe
 * Navigation Compose route argument (avoids an extra request to load the
 * course detail screen).
 */
@Serializable
data class Course(
    val id: Int,
    val name: String,
    val description: String,
    val thumbnail: String,
    val slug: String
)