package com.qlsk.service;

import com.qlsk.entity.Category;
import com.qlsk.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final com.qlsk.repository.EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
    }

    @Transactional
    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        Category category = Category.builder()
                .name(name.trim())
                .description(description)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, String name, String description) {
        Category category = getCategoryById(id);
        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByName(name)) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        category.setName(name.trim());
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        if (eventRepository.existsByCategoryId(id)) {
            throw new RuntimeException("Không thể xóa danh mục này vì đang có sự kiện thuộc danh mục!");
        }
        categoryRepository.delete(category);
    }
}
