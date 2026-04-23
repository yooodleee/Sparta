package com.example.delivery.area.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class AreaAlreadyDeletedException extends BusinessException {

    public AreaAlreadyDeletedException() {
        super(ErrorCode.AREA_ALREADY_DELETED);
    }
}
