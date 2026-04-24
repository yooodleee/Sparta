package com.example.delivery.global.common.pageable;

import java.util.Set;

public class PageSizePolicy {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_PAGE_SIZE = 10;

    public static int validate(int pageSize) {
        return ALLOWED_PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }
}
