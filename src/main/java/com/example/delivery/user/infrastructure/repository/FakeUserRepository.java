package com.example.delivery.user.infrastructure.repository;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class FakeUserRepository implements UserRepository {

    private final Map<String, UserEntity> store = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        List.of(
                UserEntity.builder()
                        .username("user01")
                        .nickname("홍길동")
                        .email("user01@example.com")
                        .role(UserRole.CUSTOMER)
                        .build(),
                UserEntity.builder()
                        .username("owner01")
                        .nickname("사장님")
                        .email("owner01@example.com")
                        .role(UserRole.OWNER)
                        .build(),
                UserEntity.builder()
                        .username("master01")
                        .nickname("최종관리자")
                        .email("master01@example.com")
                        .role(UserRole.MASTER)
                        .build()
        ).forEach(user -> store.put(user.getUsername(), user));
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return Optional.ofNullable(store.get(username));
    }
}
