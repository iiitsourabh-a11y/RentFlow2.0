package com.rentflow.backend.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.backend.user.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PropertyControllerIT {

    @Autowired private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        propertyRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ─── Helper methods ──────────────────────────────────────────────

    private String registerAndLogin(String name, String email, String role) throws Exception {
        String regBody = String.format("""
            {
                "fullName": "%s",
                "email": "%s",
                "password": "password123",
                "role": "%s"
            }
            """, name, email, role);

        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(regBody))
            .andExpect(status().isCreated());

        String loginBody = String.format("""
            {
                "email": "%s",
                "password": "password123"
            }
            """, email);

        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("token").asText();
    }

    private static final String VALID_PROPERTY = """
        {
            "title": "Cozy Apartment",
            "description": "A nice place to live",
            "rentAmount": 1500.00,
            "propertyType": "Apartment",
            "address": "123 Main St",
            "city": "Mumbai",
            "bedrooms": 2,
            "bathrooms": 1
        }
        """;

    // ─── Create property ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/properties")
    class CreateProperty {

        @Test
        @DisplayName("owner can create a property")
        void ownerCanCreate() throws Exception {
            String token = registerAndLogin("Owner One", "owner@test.com", "OWNER");

            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Cozy Apartment"))
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.rentAmount").value(1500.00))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.ownerId").isNumber())
                .andExpect(jsonPath("$.ownerName").value("Owner One"));
        }

        @Test
        @DisplayName("tenant cannot create a property")
        void tenantCannotCreate() throws Exception {
            String token = registerAndLogin("Tenant One", "tenant@test.com", "TENANT");

            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated user cannot create a property")
        void unauthenticatedCannotCreate() throws Exception {
            mvc.perform(post("/api/v1/properties")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("invalid property data returns 400")
        void invalidPropertyData() throws Exception {
            String token = registerAndLogin("Owner Two", "owner2@test.com", "OWNER");

            String invalidBody = """
                {
                    "title": "",
                    "rentAmount": -100,
                    "propertyType": "",
                    "address": "",
                    "city": "",
                    "bedrooms": -1,
                    "bathrooms": -1
                }
                """;

            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
                .andExpect(status().isBadRequest());
        }
    }

    // ─── List properties ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/properties")
    class ListProperties {

        @Test
        @DisplayName("list returns paginated results")
        void listProperties() throws Exception {
            String token = registerAndLogin("Lister Owner", "lister@test.com", "OWNER");

            // Create two properties
            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated());

            String secondProperty = """
                {
                    "title": "Luxury Villa",
                    "description": "Beautiful villa",
                    "rentAmount": 5000.00,
                    "propertyType": "Villa",
                    "address": "456 Beach Rd",
                    "city": "Goa",
                    "bedrooms": 4,
                    "bathrooms": 3
                }
                """;
            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(secondProperty))
                .andExpect(status().isCreated());

            // List all (authenticated)
            mvc.perform(get("/api/v1/properties")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        @DisplayName("anonymous user can list properties without auth")
        void anonymousUserCanListProperties() throws Exception {
            String token = registerAndLogin("Public Owner", "public@test.com", "OWNER");

            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated());

            mvc.perform(get("/api/v1/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("filter by city works")
        void filterByCity() throws Exception {
            String token = registerAndLogin("City Owner", "city@test.com", "OWNER");

            mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated());

            mvc.perform(get("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].city").value("Mumbai"));

            mvc.perform(get("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .param("city", "NonExistentCity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    // ─── Get single property ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/properties/{id}")
    class GetProperty {

        @Test
        @DisplayName("get existing property returns it")
        void getExisting() throws Exception {
            String token = registerAndLogin("Get Owner", "getowner@test.com", "OWNER");

            MvcResult createResult = mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andReturn();

            long id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

            mvc.perform(get("/api/v1/properties/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Cozy Apartment"));
        }

        @Test
        @DisplayName("anonymous user can get single property without auth")
        void anonymousCanGetSingleProperty() throws Exception {
            String token = registerAndLogin("Single Owner", "single@test.com", "OWNER");

            MvcResult createResult = mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andReturn();

            long id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

            mvc.perform(get("/api/v1/properties/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        }

        @Test
        @DisplayName("get non-existent property returns 404")
        void getNonExistent() throws Exception {
            String token = registerAndLogin("Miss Owner", "miss@test.com", "OWNER");

            mvc.perform(get("/api/v1/properties/99999")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        }
    }

    // ─── Update property ─────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/properties/{id}")
    class UpdateProperty {

        @Test
        @DisplayName("owner can update their own property")
        void ownerCanUpdate() throws Exception {
            String token = registerAndLogin("Update Owner", "update@test.com", "OWNER");

            MvcResult createResult = mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andReturn();

            long id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

            String updatedBody = """
                {
                    "title": "Updated Title",
                    "description": "Updated description",
                    "rentAmount": 2000.00,
                    "propertyType": "Apartment",
                    "address": "123 Main St",
                    "city": "Delhi",
                    "bedrooms": 3,
                    "bathrooms": 2
                }
                """;

            mvc.perform(put("/api/v1/properties/" + id)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.rentAmount").value(2000.00))
                .andExpect(jsonPath("$.city").value("Delhi"));
        }

        @Test
        @DisplayName("owner CANNOT update another owner's property")
        void cannotUpdateOthersProperty() throws Exception {
            String ownerToken = registerAndLogin("Owner A", "ownera@test.com", "OWNER");
            String otherToken = registerAndLogin("Owner B", "ownerb@test.com", "OWNER");

            MvcResult createResult = mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andReturn();

            long id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

            mvc.perform(put("/api/v1/properties/" + id)
                    .header("Authorization", "Bearer " + otherToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isForbidden());
        }
    }

    // ─── Delete property ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/properties/{id}")
    class DeleteProperty {

        @Test
        @DisplayName("owner can delete their own property")
        void ownerCanDelete() throws Exception {
            String token = registerAndLogin("Del Owner", "delowner@test.com", "OWNER");

            MvcResult createResult = mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andReturn();

            long id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

            mvc.perform(delete("/api/v1/properties/" + id)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

            // Confirm it's gone
            mvc.perform(get("/api/v1/properties/" + id)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("owner CANNOT delete another owner's property")
        void cannotDeleteOthersProperty() throws Exception {
            String ownerToken = registerAndLogin("Del A", "dela@test.com", "OWNER");
            String otherToken = registerAndLogin("Del B", "delb@test.com", "OWNER");

            MvcResult createResult = mvc.perform(post("/api/v1/properties")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_PROPERTY))
                .andExpect(status().isCreated())
                .andReturn();

            long id = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asLong();

            mvc.perform(delete("/api/v1/properties/" + id)
                    .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
        }
    }
}
