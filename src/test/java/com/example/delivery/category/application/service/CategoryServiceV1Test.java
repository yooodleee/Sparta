package com.example.delivery.category.application.service;

import com.example.delivery.category.application.exception.CategoryAlreadyExistsException;
import com.example.delivery.category.application.exception.CategoryNotFoundException;
import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import com.example.delivery.category.presentation.dto.request.ReqCreateCategoryDto;
import com.example.delivery.category.presentation.dto.request.ReqUpdateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResCreateCategoryDto;
import com.example.delivery.category.presentation.dto.response.ResGetCategoryDto;
import com.example.delivery.global.common.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceV1Test {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceV1 categoryService;

    @Nested
    @DisplayName("카테고리 생성 테스트")
    class CreateCategoryTest {

        @Test
        @DisplayName("카태고리 생성 성공")
        void createCategory_success() throws Exception {
            // given
            ReqCreateCategoryDto request = new ReqCreateCategoryDto("한식");

            CategoryEntity saved = createCategoryEntity("한식");

            given(categoryRepository.findByName("한식")).willReturn(Optional.empty());
            given(categoryRepository.save(any(CategoryEntity.class))).willReturn(saved);

            // when
            ResCreateCategoryDto result = categoryService.createCategory(request);

            // then
            assertThat(result.categoryId()).isEqualTo(saved.getId());
            assertThat(result.name()).isEqualTo("한식");
            assertThat(result.createdBy()).isEqualTo("manager01");

            ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("한식");
        }

        @Test
        @DisplayName("이미 존재하는 카테고리 생성 시 실패")
        void createCategory_fail_duplicateName() {
            // given
            ReqCreateCategoryDto request = new ReqCreateCategoryDto("한식");

            CategoryEntity existed = CategoryEntity.builder()
                    .name("한식")
                    .build();

            given(categoryRepository.findByName("한식")).willReturn(Optional.of(existed));

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(CategoryAlreadyExistsException.class);

            verify(categoryRepository, never()).save(any(CategoryEntity.class));
        }
    }

    @Nested
    @DisplayName("카테고리 목록 조회 테스트")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("키워드 없이 전체 카테고리 목록 조회 성공")
        void getAllCategories_success_withoutKeyword() throws Exception {
            // given
            CategoryEntity category1 = createCategoryEntity("한식");
            CategoryEntity category2 = createCategoryEntity("중식");

            Page<CategoryEntity> page = new PageImpl<>(
                    List.of(category1, category2),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    2
            );

            given(categoryRepository.findAll(any(PageRequest.class))).willReturn(page);

            // when
            PageResponse<ResGetCategoryDto> result = categoryService.getAllCategories(null, 0, 10);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).name()).isEqualTo("한식");
            assertThat(result.content().get(1).name()).isEqualTo("중식");
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.totalPages()).isEqualTo(1);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("키워드로 카테고리 목록 조회 성공")
        void getAllCategories_success_withKeyword() throws Exception {
            // given
            CategoryEntity category = createCategoryEntity("한식");

            Page<CategoryEntity> page = new PageImpl<>(
                    List.of(category),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );

            given(categoryRepository.findByNameContaining(eq("한"), any(Pageable.class))).willReturn(page);

            // when
            PageResponse<ResGetCategoryDto> result = categoryService.getAllCategories("한", 0, 10);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).name()).isEqualTo("한식");
        }

        @Test
        @DisplayName("허용되지 않은 size는 10으로 보정")
        void getAllCategories_invalidSize_defaultToTen() {
            // given
            Page<CategoryEntity> empty = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );

            given(categoryRepository.findAll(any(PageRequest.class))).willReturn(empty);

            // when
            categoryService.getAllCategories(null, 0, 7);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(categoryRepository).findAll(captor.capture());

            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("음수 page는 0으로 보정")
        void getAllCategories_invalidPage_defaultToZero() {
            // given
            Page<CategoryEntity> empty = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    0
            );

            given(categoryRepository.findAll(any(Pageable.class))).willReturn(empty);

            // when
            categoryService.getAllCategories(null, -1, 10);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(categoryRepository).findAll(captor.capture());

            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("카테고리 상세 조회 테스트")
    class GetCategoryTest {

        @Test
        @DisplayName("카테고리 상세 조회 성공")
        void getCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            CategoryEntity category = createCategoryEntity("한식");
            setField(category, "id", categoryId);

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

            // when
            ResGetCategoryDto result = categoryService.getCategory(categoryId);

            // then
            assertThat(result.categoryId()).isEqualTo(categoryId);
            assertThat(result.name()).isEqualTo("한식");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 시 상세 조회 실패")
        void getCategory_fail_notFound() {
            // given
            UUID categoryId = UUID.randomUUID();
            given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> categoryService.getCategory(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("카테고리 수정 테스트")
    class updateCategoryTest {

        @Test
        @DisplayName("카테고리 수정 성공")
        void updateCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            CategoryEntity category = createCategoryEntity("한식");
            setField(category, "id", categoryId);

            ReqUpdateCategoryDto request = new ReqUpdateCategoryDto("중식");

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(categoryRepository.findByName("중식")).willReturn(Optional.empty());

            // when
            ResGetCategoryDto result = categoryService.updateCategory(categoryId, request);

            // then
            assertThat(result.name()).isEqualTo("중식");
            assertThat(category.getName()).isEqualTo("중식");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리면 수정 실패")
        void updateCategory_fail_notFound() {
            // given
            UUID categoryId = UUID.randomUUID();
            ReqUpdateCategoryDto request = new ReqUpdateCategoryDto("중식");

            given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                    .isInstanceOf(CategoryNotFoundException.class);

            verify(categoryRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("이미 존재하는 카테고리명일 시 수정 실패")
        void updateCategory_fail_duplicateName() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            CategoryEntity category = createCategoryEntity("한식");
            setField(category, "id", categoryId);

            CategoryEntity exist = createCategoryEntity("중식");

            ReqUpdateCategoryDto request = new ReqUpdateCategoryDto("중식");

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(categoryRepository.findByName("중식")).willReturn(Optional.of(exist));

            //when & then
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                    .isInstanceOf(CategoryAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제 테스트")
    class deleteCategoryTest {

        @Test
        @DisplayName("카테고리 삭제 성공")
        void deleteCategory_success() throws Exception {
            // given
            UUID categoryId = UUID.randomUUID();
            CategoryEntity category = createCategoryEntity("한식");
            setField(category, "id", categoryId);

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

            // when
            categoryService.deleteCategory(categoryId, "master01");

            // then
            assertThat(category.isDeleted()).isTrue();
            assertThat(getField(category, "deletedBy")).isEqualTo("master01");
            assertThat(getField(category, "deletedAt")).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 시 삭제 실패")
        void deleteCategory_fail_notFound() {
            // given
            UUID categoryId = UUID.randomUUID();
            given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, "master01"))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    /**
     * Category 데이터 생성 메서드
     */
    private CategoryEntity createCategoryEntity(String name) throws Exception {
        CategoryEntity category = CategoryEntity.builder()
                .name(name)
                .build();

        setField(category, "id", UUID.randomUUID());
        setBaseEntityFields(
                category,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "manager01",
                LocalDateTime.of(2026, 1, 1, 12, 0),
                "manager01"
        );

        return category;
    }

    /**
     * BaseEntity 기본 설정 메서드
     */
    private void setBaseEntityFields(
            Object target,
            LocalDateTime createdAt,
            String createdBy,
            LocalDateTime updatedAt,
            String updatedBy
    ) throws Exception{
        setField(target, "createdAt", createdAt);
        setField(target, "createdBy", createdBy);
        setField(target, "updatedAt", updatedAt);
        setField(target, "updatedBy", updatedBy);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Class<?> clazz = target.getClass();

        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            }  catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
            throw new NoSuchFieldException(fieldName);
    }
}