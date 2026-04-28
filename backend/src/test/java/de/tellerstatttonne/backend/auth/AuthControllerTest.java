package de.tellerstatttonne.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.Filter;

@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context);
        for (Filter filter : securityFilterChain.getFilters()) {
            builder = builder.addFilters(filter);
        }
        mockMvc = builder.build();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_login_me_refresh_logout_flow() throws Exception {
        String registerBody = """
            {"email":"alice@example.de","password":"secret123"}
            """;

        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("alice@example.de"))
            .andExpect(jsonPath("$.user.role").value("USER"))
            .andReturn();

        JsonNode regJson = objectMapper.readTree(registered.getResponse().getContentAsString());
        String accessToken = regJson.get("accessToken").asText();
        String refreshToken = regJson.get("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@example.de"));

        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());

        String loginBody = """
            {"email":"alice@example.de","password":"secret123"}
            """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.de\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());

        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode refJson = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        String newRefresh = refJson.get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isUnauthorized());

        String logoutBody = "{\"refreshToken\":\"" + newRefresh + "\"}";
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON).content(logoutBody))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(logoutBody))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationFails() throws Exception {
        String body = "{\"email\":\"bob@example.de\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/partners"))
            .andExpect(status().isUnauthorized());
    }
}
