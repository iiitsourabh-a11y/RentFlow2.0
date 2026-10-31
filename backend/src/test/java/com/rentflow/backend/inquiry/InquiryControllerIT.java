package com.rentflow.backend.inquiry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.backend.property.PropertyRepository;
import com.rentflow.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InquiryControllerIT {

    @Autowired private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        inquiryRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();
    }

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

    private Long createTestProperty(String ownerToken) throws Exception {
        String body = """
            {
                "title": "Sunny Loft",
                "description": "Great view",
                "rentAmount": 1200.00,
                "propertyType": "Apartment",
                "address": "100 Skyline Blvd",
                "city": "Bengaluru",
                "bedrooms": 1,
                "bathrooms": 1
            }
            """;

        MvcResult res = mvc.perform(post("/api/v1/properties")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString())
            .get("id").asLong();
    }

    @Test
    @DisplayName("tenant can submit an inquiry for a property")
    void tenantCanSubmitInquiry() throws Exception {
        String ownerToken = registerAndLogin("Owner Bob", "bob.owner@test.com", "OWNER");
        String tenantToken = registerAndLogin("Tenant Jane", "jane.tenant@test.com", "TENANT");
        Long propId = createTestProperty(ownerToken);

        String inquiryBody = """
            {
                "message": "Hi, is this apartment available for move-in next month?"
            }
            """;

        mvc.perform(post("/api/v1/inquiries/property/" + propId)
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inquiryBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.propertyId").value(propId))
            .andExpect(jsonPath("$.propertyTitle").value("Sunny Loft"))
            .andExpect(jsonPath("$.tenantName").value("Tenant Jane"))
            .andExpect(jsonPath("$.tenantEmail").value("jane.tenant@test.com"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("owner cannot submit an inquiry")
    void ownerCannotSubmitInquiry() throws Exception {
        String ownerToken = registerAndLogin("Owner Tim", "tim@test.com", "OWNER");
        Long propId = createTestProperty(ownerToken);

        mvc.perform(post("/api/v1/inquiries/property/" + propId)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"I want to rent this\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("unauthenticated user cannot submit inquiry")
    void unauthenticatedCannotSubmit() throws Exception {
        mvc.perform(post("/api/v1/inquiries/property/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Hello\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("empty inquiry message returns 400")
    void emptyMessageReturns400() throws Exception {
        String ownerToken = registerAndLogin("Owner Sara", "sara@test.com", "OWNER");
        String tenantToken = registerAndLogin("Tenant Dan", "dan@test.com", "TENANT");
        Long propId = createTestProperty(ownerToken);

        mvc.perform(post("/api/v1/inquiries/property/" + propId)
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"   \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tenant can list sent inquiries")
    void tenantCanListSent() throws Exception {
        String ownerToken = registerAndLogin("Owner Mark", "mark@test.com", "OWNER");
        String tenantToken = registerAndLogin("Tenant Lucy", "lucy@test.com", "TENANT");
        Long propId = createTestProperty(ownerToken);

        mvc.perform(post("/api/v1/inquiries/property/" + propId)
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Interested in this place!\"}"))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/inquiries/sent")
                .header("Authorization", "Bearer " + tenantToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].propertyTitle").value("Sunny Loft"));
    }

    @Test
    @DisplayName("owner can list received inquiries and update status")
    void ownerCanListAndManageInquiries() throws Exception {
        String ownerToken = registerAndLogin("Owner Leo", "leo@test.com", "OWNER");
        String tenantToken = registerAndLogin("Tenant Sam", "sam@test.com", "TENANT");
        Long propId = createTestProperty(ownerToken);

        MvcResult inqRes = mvc.perform(post("/api/v1/inquiries/property/" + propId)
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Can I visit tomorrow?\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        long inquiryId = objectMapper.readTree(inqRes.getResponse().getContentAsString())
            .get("id").asLong();

        // Owner views received inquiries
        mvc.perform(get("/api/v1/inquiries/received")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(inquiryId));

        // Owner accepts the inquiry
        mvc.perform(patch("/api/v1/inquiries/" + inquiryId + "/status")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"ACCEPTED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("other owner cannot modify inquiry for property they do not own")
    void otherOwnerCannotModifyInquiry() throws Exception {
        String ownerA = registerAndLogin("Owner Alpha", "alpha@test.com", "OWNER");
        String ownerB = registerAndLogin("Owner Beta", "beta@test.com", "OWNER");
        String tenant = registerAndLogin("Tenant Tom", "tom@test.com", "TENANT");

        Long propId = createTestProperty(ownerA);

        MvcResult inqRes = mvc.perform(post("/api/v1/inquiries/property/" + propId)
                .header("Authorization", "Bearer " + tenant)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Inquiry for Alpha\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        long inquiryId = objectMapper.readTree(inqRes.getResponse().getContentAsString())
            .get("id").asLong();

        // Owner Beta tries to modify Alpha's inquiry
        mvc.perform(patch("/api/v1/inquiries/" + inquiryId + "/status")
                .header("Authorization", "Bearer " + ownerB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"REJECTED\"}"))
            .andExpect(status().isForbidden());
    }
}
