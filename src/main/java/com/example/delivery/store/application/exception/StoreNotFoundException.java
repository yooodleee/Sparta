package com.example.delivery.store.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class StoreNotFoundException extends BusinessException {

    public StoreNotFoundException() {
        super(ErrorCode.STORE_NOT_FOUND);
    }
}
