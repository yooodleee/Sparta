package com.example.delivery.global.test;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.delivery.user.domain.entity.UserEntity;
import com.example.delivery.user.domain.entity.UserRole;
import com.example.delivery.user.domain.repository.UserRepository;
import com.example.delivery.user.domain.vo.Email;
import com.example.delivery.user.domain.vo.Username;
import com.example.delivery.user.presentation.dto.request.ReqLogin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 공통 통합 테스트 베이스.
 *
 * - MockMvc/ObjectMapper/Repository/PasswordEncoder를 묶어 헬퍼만 노출.
 * - {@code @SpringBootTest} 등 클래스 메타는 구체 테스트 클래스에서 직접 선언한다
 *   (트랜잭션 정책이 시나리오마다 달라 베이스에서 강제하지 않는다).
 */
public abstract class IntegrationTestSupport {

    public static final String DEFAULT_PASSWORD = "Abcd1234!";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReqLogin(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.path("data").path("accessToken").asText();
    }

    protected UserEntity seedUser(String username, UserRole role) {
        return seedUser(username, username + "_nick", username + "@example.com", DEFAULT_PASSWORD, role);
    }

    protected UserEntity seedUser(String username, String nickname, String email,
            String rawPassword, UserRole role) {
        return userRepository.save(UserEntity.register(
                new Username(username),
                nickname,
                new Email(email),
                passwordEncoder.encode(rawPassword),
                role));
    }

    protected ResultActions authedGet(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    protected ResultActions authedPatch(String url, String token, Object body) throws Exception {
        return mockMvc.perform(patch(url)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions authedDelete(String url, String token) throws Exception {
        return mockMvc.perform(delete(url).header("Authorization", "Bearer " + token));
    }
}
