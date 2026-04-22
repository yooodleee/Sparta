package com.example.delivery.user.domain.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.jpa.domain.Specification;

public final class UserSearchSpecs {

    private UserSearchSpecs() {
    }

    public static Specification<UserEntity> build(String keyword, UserRole role) {
        return (root, query, cb) -> {
            Predicate[] predicates = Stream.of(
                            keywordPredicate(keyword, root, cb),
                            rolePredicate(role, root, cb))
                    .flatMap(Optional::stream)
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }

    private static Optional<Predicate> keywordPredicate(String keyword,
            Root<UserEntity> root, CriteriaBuilder cb) {
        return Optional.ofNullable(keyword)
                .filter(k -> !k.isBlank())
                .map(k -> "%" + k.toLowerCase(Locale.ROOT) + "%")
                .map(like -> cb.or(
                        cb.like(root.get("username"), like),
                        cb.like(cb.lower(root.get("nickname")), like),
                        cb.like(root.get("email"), like)));
    }

    private static Optional<Predicate> rolePredicate(UserRole role,
            Root<UserEntity> root, CriteriaBuilder cb) {
        return Optional.ofNullable(role)
                .map(r -> cb.equal(root.get("role"), r));
    }
}
