package com.example.delivery.store.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class RelatedAreaNotFoundException extends BusinessException {

    public RelatedAreaNotFoundException() {
        super(ErrorCode.AREA_NOT_FOUND);
    }
}
