package com.example.delivery.store.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class StoreAccessDeniedException extends BusinessException {

    public StoreAccessDeniedException() {
        super(ErrorCode.STORE_ACCESS_DENIED);
    }
}
