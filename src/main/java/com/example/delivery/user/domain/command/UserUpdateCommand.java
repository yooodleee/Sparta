package com.example.delivery.user.domain.command;

import com.example.delivery.user.domain.vo.Email;
import java.util.Optional;

public record UserUpdateCommand(
        Optional<String> nickname,
        Optional<Email> email,
        Optional<String> newPasswordHash,
        Optional<Boolean> isPublic
) {

}
