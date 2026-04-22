package com.example.delivery.user.domain.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class UserSearchSpecs {

    private UserSearchSpecs() {
    }

    public static Specification<UserEntity> build(String keyword, UserRole role) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("nickname")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
