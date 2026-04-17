package com.example.delivery.global.infrastructure.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// WARN: username을 저장하므로 BaseEntity.createdBy/updatedBy length(=10)을 넘는 이름이 들어오면 저장이 실패한다.
// 이후 length 확장 또는 PK 저장 방식으로 전환 검토.
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM);
        }
        return Optional.of(authentication.getName());
    }
}
