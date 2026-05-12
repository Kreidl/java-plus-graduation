package ru.practicum.category.mapper;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CategoryMapper {
    public Category mapToEntity(NewCategoryDto newCategoryDto) {
        return new Category(null, newCategoryDto.name());
    }

    public CategoryDto mapToDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
