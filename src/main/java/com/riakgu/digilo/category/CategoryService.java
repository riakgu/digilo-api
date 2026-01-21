package com.riakgu.digilo.category;

import com.riakgu.digilo.category.dto.CategoryRequest;
import com.riakgu.digilo.category.dto.CategoryResponse;
import com.riakgu.digilo.common.exception.DuplicateResourceException;
import com.riakgu.digilo.common.exception.NotFoundException;
import com.riakgu.digilo.common.exception.UnauthorizedException;
import com.riakgu.digilo.common.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryResponse create(CategoryRequest request) {

        String newName = request.getName().trim();
        String newSlug = SlugUtil.normalize(request.getSlug());

        if (categoryRepository.existsByName(newName)) {
            throw new DuplicateResourceException("Category with name " + newName + " already exists");
        }

        if (categoryRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Category with slug " + newSlug + " already exists");
        }

        Category category = Category.builder()
                .name(newName)
                .slug(newSlug)
                .description(request.getDescription())
                .build();

        categoryRepository.save(category);

        return CategoryResponse.fromEntity(category);
    }

    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Category with slug " + slug + " not found")) ;

        return CategoryResponse.fromEntity(category);
    }

    public CategoryResponse getById(Long id) {
        Category category= categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));

        return CategoryResponse.fromEntity(category);
    }

    public CategoryResponse update(CategoryRequest request, Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));

        String newName = request.getName().trim();
        String newSlug = SlugUtil.normalize(request.getSlug());

        if (!category.getName().equalsIgnoreCase(newName)) {
            if (categoryRepository.existsByName(newName)) {
                throw new DuplicateResourceException("Category with name " + newName + " already exists");
            }
        }

        if (!category.getSlug().equalsIgnoreCase(newSlug)) {
            if (categoryRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Category with slug " + newSlug + " already exists");
            }
        }

        category.setName(newName);
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());

        categoryRepository.save(category);

        return CategoryResponse.fromEntity(category);
    }
}
