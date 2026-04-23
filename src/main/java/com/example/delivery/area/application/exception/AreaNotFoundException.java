package com.example.delivery.area.application.exception;

import com.example.delivery.global.common.exception.BusinessException;
import com.example.delivery.global.common.exception.ErrorCode;

public class AreaNotFoundException extends BusinessException {

    public AreaNotFoundException() {
      super(ErrorCode.AREA_NOT_FOUND);
    }
}
