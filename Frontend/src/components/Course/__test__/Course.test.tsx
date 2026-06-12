import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { Course } from "../Course";

describe("Course Component", () => {
  const mockCourse = {
    id: 1,
    name: "React Fundamentals",
    description: "Aprende los fundamentos de React desde cero",
    thumbnail: "https://example.com/thumbnail.jpg",
  };

  it("renders course information correctly", () => {
    render(<Course {...mockCourse} />);

    // Check if name is rendered
    expect(screen.getByText(mockCourse.name)).toBeDefined();

    // Check if description is rendered
    expect(screen.getByText(mockCourse.description)).toBeDefined();
  });

  it("renders thumbnail with correct alt text", () => {
    render(<Course {...mockCourse} />);

    const thumbnail = screen.getByRole("img");
    expect(thumbnail).toHaveAttribute("src", mockCourse.thumbnail);
    expect(thumbnail).toHaveAttribute("alt", mockCourse.name);
  });

  it("renders with correct structure", () => {
    const { container } = render(<Course {...mockCourse} />);

    // Check if the main article exists
    expect(container.querySelector("article")).toBeDefined();

    // Check if the thumbnail container exists
    expect(container.querySelector("div > img")).toBeDefined();

    // Check if the course info section exists
    expect(container.querySelector("div > h2")).toBeDefined();
    expect(container.querySelector("div > p")).toBeDefined();
  });

  describe("Rating section", () => {
    it("renders rating when average_rating is provided", () => {
      const courseWithRating = {
        ...mockCourse,
        average_rating: 4.5,
        total_ratings: 120,
      };

      render(<Course {...courseWithRating} />);

      expect(screen.getByText("(120)")).toBeInTheDocument();
      expect(screen.getByRole("img", { name: /Rating: 4.5 out of 5 stars/i })).toBeInTheDocument();
    });

    it("does not render rating section when average_rating is not provided", () => {
      render(<Course {...mockCourse} />);

      expect(screen.queryByText(/^\(\d+\)$/)).not.toBeInTheDocument();
    });

    it("renders rating correctly when average_rating is zero", () => {
      const courseWithZeroRating = {
        ...mockCourse,
        average_rating: 0,
        total_ratings: 0,
      };

      render(<Course {...courseWithZeroRating} />);

      // average_rating === 0 es un number válido, debe renderizar el rating
      expect(
        screen.getByRole("img", { name: /Rating: 0.0 out of 5 stars/i })
      ).toBeInTheDocument();
    });
  });
});
