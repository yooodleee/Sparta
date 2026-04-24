package com.example.delivery.category.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class CategoryAlreadyExistsException extends BusinessException {

    public CategoryAlreadyExistsException() {
        super(ErrorCode.CATEGORY_ALREADY_EXISTS);
    }
}
