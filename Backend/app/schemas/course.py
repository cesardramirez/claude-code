"""
Pydantic schemas for course and class (lesson) responses.
"""
from pydantic import BaseModel, Field
from typing import Dict, List


class CourseResponse(BaseModel):
    """
    Schema for course summary, as returned by GET /courses.
    """
    id: int
    name: str
    description: str
    thumbnail: str
    slug: str
    average_rating: float = Field(
        ...,
        ge=0.0,
        le=5.0,
        description="Average rating (0.0 if no ratings)"
    )
    total_ratings: int = Field(
        ...,
        ge=0,
        description="Total number of active ratings"
    )

    class Config:
        from_attributes = True


class CourseClassSummary(BaseModel):
    """
    Schema for a lesson/class summary nested in course detail.
    """
    id: int
    name: str
    description: str
    slug: str


class CourseDetailResponse(BaseModel):
    """
    Schema for course detail, as returned by GET /courses/{slug}.
    """
    id: int
    name: str
    description: str
    thumbnail: str
    slug: str
    teacher_id: List[int]
    classes: List[CourseClassSummary]
    average_rating: float = Field(
        ...,
        ge=0.0,
        le=5.0,
        description="Average rating (0.0 if no ratings)"
    )
    total_ratings: int = Field(
        ...,
        ge=0,
        description="Total number of active ratings"
    )
    rating_distribution: Dict[int, int] = Field(
        ...,
        description="Count of ratings per value (1-5)"
    )


class ClassDetailResponse(BaseModel):
    """
    Schema for lesson/class detail, as returned by GET /classes/{class_id}.
    """
    id: int
    title: str
    description: str
    slug: str
    video: str
    duration: int
