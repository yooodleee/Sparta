package com.example.delivery.category.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException() {
      super(ErrorCode.CATEGORY_NOT_FOUND);
    }
}
