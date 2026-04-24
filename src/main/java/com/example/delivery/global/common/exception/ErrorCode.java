package com.example.delivery.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ROLE_MISMATCH(HttpStatus.FORBIDDEN, "토큰의 권한이 현재 권한과 일치하지 않습니다."),
    SIGNUP_ROLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "회원가입으로 부여할 수 없는 권한입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "아이디는 소문자/숫자 4~10자여야 합니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 8~15자, 대/소문자·숫자·특수문자를 각 1자 이상 포함해야 합니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    CANNOT_DELETE_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 삭제할 수 없습니다."),
    CURRENT_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비밀번호 변경 시 현재 비밀번호가 필요합니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),

    //Menu
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "가게를 찾을 수 없습니다."),

    //AI
    AI_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "AI 요청 기록을 찾을 수 없습니다."),
    AI_DESCRIPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 설명 생성에 실패했습니다."),

    //Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "주문 상태를 변경할 수 없습니다."),
    ORDER_CANCEL_TIMEOUT(HttpStatus.BAD_REQUEST, "주문 생성 후 5분이 경과하여 취소할 수 없습니다."),

    //Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "해당 주문에 이미 처리된 결제가 존재합니다."),
    PAYMENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결제 처리 중 오류가 발생했습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 결제 금액입니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "현재 결제 상태에서는 해당 작업을 수행할 수 없습니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 해당 주문에 리뷰를 작성했습니다."),
    ORDER_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 주문에만 리뷰를 작성할 수 있습니다."),

    // Store
    STORE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 가게에 대한 권한이 없습니다."),
    STORE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "가게명은 필수입니다."),
    STORE_ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST, "가게 주소는 필수입니다."),
    STORE_AREA_REQUIRED(HttpStatus.BAD_REQUEST, "지역 ID는 필수입니다."),
    STORE_CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "카테고리 ID는 필수입니다."),
    INVALID_STORE_RATING(HttpStatus.BAD_REQUEST, "가게 평균 평점 값이 올바르지 않습니다."),
    STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 소유자가 이미 동일한 이름의 가게를 보유하고 있습니다."),

    // Area
    AREA_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "삭제된 지역명입니다."),
    AREA_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다."),
    AREA_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 지역명입니다."),

    // Category
    CATEGORY_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "삭제된 카테고리 이름입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.");


    private final HttpStatus status;
    private final String message;

    public int code() {
        return status.value();
    }
}
