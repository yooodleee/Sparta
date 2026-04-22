package com.example.delivery.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.presentation.dto.request.ReqLogin;
import com.example.delivery.user.presentation.dto.request.ReqSignup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 → DB에 유저 저장 (201)")
    void signup_success() throws Exception {
        ReqSignup req = new ReqSignup(
                "alice01", "앨리스", "alice@example.com",
                "Abcd1234!", UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("alice01"));

        assertThat(userRepository.findByUsername("alice01")).isPresent();
    }

    @Test
    @DisplayName("로그인 성공 + 비밀번호 오류 시 401")
    void login_successAndFailure() throws Exception {
        userRepository.save(UserEntity.register(
                new Username("bob01"), "밥", new Email("bob@example.com"),
                passwordEncoder.encode("Abcd1234!"), UserRole.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReqLogin("bob01", "Abcd1234!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ReqLogin("bob01", "WrongPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 발급 후 DB role 변경 → 재요청 403 (ROLE_MISMATCH)")
    void roleMismatch_blocks() throws Exception {
        UserEntity saved = userRepository.save(UserEntity.register(
                new Username("cara01"), "카라", new Email("cara@example.com"),
                passwordEncoder.encode("Abcd1234!"), UserRole.CUSTOMER));

        String token = login("cara01", "Abcd1234!");

        saved.changeRole(UserRole.OWNER);
        userRepository.save(saved);

        mockMvc.perform(get("/api/v1/users/cara01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("JWT 발급 후 soft delete → 재요청 401")
    void softDeleted_blocks() throws Exception {
        UserEntity saved = userRepository.save(UserEntity.register(
                new Username("dave01"), "데이브", new Email("dave@example.com"),
                passwordEncoder.encode("Abcd1234!"), UserRole.CUSTOMER));

        String token = login("dave01", "Abcd1234!");

        saved.softDelete("dave01");
        userRepository.save(saved);

        mockMvc.perform(get("/api/v1/users/dave01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReqLogin(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.path("data").path("accessToken").asText();
    }
}
