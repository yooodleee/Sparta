package com.example.delivery.category.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class CategoryAlreadyDeletedException extends BusinessException {

    public CategoryAlreadyDeletedException() {
        super(ErrorCode.CATEGORY_ALREADY_DELETED);
    }
}
