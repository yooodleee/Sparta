package com.example.delivery.area.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class AreaAlreadyExistsException extends BusinessException {

    public AreaAlreadyExistsException() {
        super(ErrorCode.AREA_ALREADY_EXISTS);
    }
}
