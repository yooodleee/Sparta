package com.example.delivery.category.infrastructure.init;

import com.example.delivery.category.domain.entity.CategoryEntity;
import com.example.delivery.category.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 애플리케이션 기동 시 기본 카테고리(한식/중식/분식/치킨/피자)를 삽입함
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class CategoryDataInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_CATEGORY_NAMES =
            List.of("한식", "중식", "분식", "치킨", "피자");

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> inserted = new ArrayList<>();

        for (String name : DEFAULT_CATEGORY_NAMES) {
            if (categoryRepository.findByName(name).isPresent()) {
                continue;
            }
            categoryRepository.save(CategoryEntity.builder().name(name).build());
            inserted.add(name);
        }

        if (inserted.isEmpty()) {
            log.info("[CategoryDataInitializer] 기본 카테고리가 이미 모두 존재합니다. 스킵합니다.");
        } else {
            log.info("[CategoryDataInitializer] 기본 카테고리 {}건 초기화 완료: {}",
                    inserted.size(), inserted);
        }
    }
}
