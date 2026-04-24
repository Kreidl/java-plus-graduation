package ru.practicum.category.service;

import java.util.List;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.exception.NotFoundException;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        Category category = CategoryMapper.mapToEntity(newCategoryDto);
        return CategoryMapper.mapToDto(categoryRepository.save(category));
    }

    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto updateCategoryDto) {
        Category category =
                categoryRepository
                        .findById(catId)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Category with id=%d was not found", catId));
        category.setName(updateCategoryDto.name());
        return CategoryMapper.mapToDto(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategoriesPaged(int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        return categoryRepository.findAll(pageable).stream().map(CategoryMapper::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Category with id=%d was not found", id));
        return CategoryMapper.mapToDto(category);
    }

    @Override
    public void deleteCategoryById(Long id) {
        categoryRepository
                .findById(id)
                .orElseThrow(NotFoundException.supplier("Category with id=%d was not found", id));
        categoryRepository.deleteById(id);
    }
}
