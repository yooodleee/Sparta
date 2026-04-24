package com.example.delivery.store.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class RelatedCategoryNotFoundException extends BusinessException {

    public RelatedCategoryNotFoundException() {
        super(ErrorCode.CATEGORY_NOT_FOUND);
    }
}
