package com.example.delivery.global.common.pageable;

import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@NoArgsConstructor
public class PageableUtils {

    private static final String DEFAULT_SORT = "createdAt";

    public static Pageable createPageable(int page, int size) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = PageSizePolicy.validate(size);

        return PageRequest.of(
                validatedPage,
                validatedSize,
                Sort.by(Sort.Direction.DESC, DEFAULT_SORT)
        );
    }

    public static Pageable applyPageSizePolicy(Pageable pageable) {
        int validatedSize = PageSizePolicy.validate(pageable.getPageSize());

        if (validatedSize == pageable.getPageSize()) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), validatedSize, pageable.getSort());
    }

    public static boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.trim().isEmpty();
    }
}
