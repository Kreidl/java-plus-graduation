package ru.practicum.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewCategoryDto(@Size(min = 1, max = 50) @NotBlank String name) {}
