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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Creating new category with name='{}'", newCategoryDto.name());
        Category category = CategoryMapper.mapToEntity(newCategoryDto);
        Category saved = categoryRepository.save(category);
        log.info("Category created successfully with id={}", saved.getId());
        return CategoryMapper.mapToDto(saved);
    }

    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto updateCategoryDto) {
        log.info("Updating category {} with new name='{}'", catId, updateCategoryDto.name());

        Category category =
                categoryRepository
                        .findById(catId)
                        .orElseThrow(
                                NotFoundException.supplier(
                                        "Category with id=%d was not found", catId));

        category.setName(updateCategoryDto.name());
        Category updated = categoryRepository.save(category);
        log.info("Category {} updated successfully", catId);
        return CategoryMapper.mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategoriesPaged(int from, int size) {
        log.debug("Fetching categories page: from={}, size={}", from, size);
        Pageable pageable = PageRequest.of(from, size);
        List<CategoryDto> result =
                categoryRepository.findAll(pageable).stream()
                        .map(CategoryMapper::mapToDto)
                        .toList();
        log.debug("Returned {} categories", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        log.debug("Fetching category by id={}", id);
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
        log.info("Deleting category with id={}", id);
        categoryRepository
                .findById(id)
                .orElseThrow(NotFoundException.supplier("Category with id=%d was not found", id));
        categoryRepository.deleteById(id);
        log.debug("Category with id={} deleted successfully", id);
    }
}
