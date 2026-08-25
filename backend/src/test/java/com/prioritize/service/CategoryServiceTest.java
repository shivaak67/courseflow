package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.prioritize.dto.CategoryRequest;
import com.prioritize.dto.CategoryResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.CategoryMapper;
import com.prioritize.model.Category;
import com.prioritize.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, new CategoryMapper());
    }

    @Test
    void createPersistsOwnedCategory() {
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(USER_A, "Academics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            if (category.getId() == null) {
                category.setId(CATEGORY_ID);
            }
            if (category.getCreatedAt() == null) {
                Instant now = Instant.parse("2026-01-01T00:00:00Z");
                category.setCreatedAt(now);
                category.setUpdatedAt(now);
            }
            return category;
        });

        CategoryResponse response = categoryService.create(
                USER_A, new CategoryRequest("Academics", "book", "#336699"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_A);
        assertThat(response.name()).isEqualTo("Academics");
        assertThat(response.icon()).isEqualTo("book");
        assertThat(response.color()).isEqualTo("#336699");
    }

    @Test
    void createRejectsDuplicateNameForUser() {
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(USER_A, "Academics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(USER_A, new CategoryRequest("Academics", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessage("Category name already exists");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.get(USER_B, CATEGORY_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    void listReturnsOnlyCallerCategories() {
        Category category = ownedCategory(USER_A);
        when(categoryRepository.findByUserIdOrderByNameAsc(USER_A)).thenReturn(List.of(category));

        List<CategoryResponse> responses = categoryService.list(USER_A);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(CATEGORY_ID);
        verify(categoryRepository, never()).findAll();
    }

    private Category ownedCategory(UUID userId) {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setUserId(userId);
        category.setName("Academics");
        category.setIcon("book");
        category.setColor("#336699");
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return category;
    }
}
