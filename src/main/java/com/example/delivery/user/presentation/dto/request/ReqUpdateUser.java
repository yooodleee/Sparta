package com.example.delivery.user.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record ReqUpdateUser(
        @Size(max = 100) String nickname,
        @Size(max = 255) String email,
        @Size(min = 8, max = 15) String password,
        Boolean isPublic
) {

}
