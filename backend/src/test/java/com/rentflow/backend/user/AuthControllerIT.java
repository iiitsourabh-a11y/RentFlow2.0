package com.rentflow.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    // ─── Registration ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("valid tenant registration returns 201")
        void registerTenant() throws Exception {
            String body = """
                {
                    "fullName": "Alice Tenant",
                    "email": "alice@example.com",
                    "password": "password123",
                    "phone": "1234567890",
                    "role": "TENANT"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fullName").value("Alice Tenant"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("valid owner registration returns 201")
        void registerOwner() throws Exception {
            String body = """
                {
                    "fullName": "Bob Owner",
                    "email": "bob@example.com",
                    "password": "password123",
                    "role": "OWNER"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"));
        }

        @Test
        @DisplayName("admin registration is forbidden")
        void registerAdminForbidden() throws Exception {
            String body = """
                {
                    "fullName": "Evil Admin",
                    "email": "admin@example.com",
                    "password": "password123",
                    "role": "ADMIN"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("duplicate email returns 409 Conflict")
        void duplicateEmail() throws Exception {
            String body = """
                {
                    "fullName": "Alice",
                    "email": "alice@example.com",
                    "password": "password123",
                    "role": "TENANT"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated());

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("missing required fields returns 400")
        void missingFields() throws Exception {
            String body = """
                {
                    "email": "noname@example.com"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("password too short returns 400")
        void shortPassword() throws Exception {
            String body = """
                {
                    "fullName": "Short Pass",
                    "email": "short@example.com",
                    "password": "abc",
                    "role": "TENANT"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid email format returns 400")
        void invalidEmail() throws Exception {
            String body = """
                {
                    "fullName": "Bad Email",
                    "email": "not-an-email",
                    "password": "password123",
                    "role": "TENANT"
                }
                """;

            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    // ─── Login ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        private void registerUser() throws Exception {
            String body = """
                {
                    "fullName": "Test User",
                    "email": "test@example.com",
                    "password": "password123",
                    "role": "TENANT"
                }
                """;
            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("valid credentials return JWT token")
        void loginSuccess() throws Exception {
            registerUser();

            String body = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

            mvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("wrong password returns 401")
        void wrongPassword() throws Exception {
            registerUser();

            String body = """
                {
                    "email": "test@example.com",
                    "password": "wrongpassword"
                }
                """;

            mvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("non-existent email returns 401")
        void nonExistentEmail() throws Exception {
            String body = """
                {
                    "email": "nobody@example.com",
                    "password": "password123"
                }
                """;

            mvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("empty body returns 400")
        void emptyBody() throws Exception {
            mvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }
    }

    // ─── /me endpoint ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class Me {

        private String loginAndGetToken() throws Exception {
            String regBody = """
                {
                    "fullName": "Me User",
                    "email": "me@example.com",
                    "password": "password123",
                    "role": "TENANT"
                }
                """;
            mvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(regBody))
                .andExpect(status().isCreated());

            String loginBody = """
                {
                    "email": "me@example.com",
                    "password": "password123"
                }
                """;
            MvcResult result = mvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

            return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        }

        @Test
        @DisplayName("authenticated user can access /me")
        void meSuccess() throws Exception {
            String token = loginAndGetToken();

            mvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.fullName").value("Me User"))
                .andExpect(jsonPath("$.role").value("TENANT"));
        }

        @Test
        @DisplayName("unauthenticated request to /me returns 401")
        void meUnauthenticated() throws Exception {
            mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("invalid token returns 401")
        void meInvalidToken() throws Exception {
            mvc.perform(get("/api/v1/auth/me")
                    .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
        }
    }
}
