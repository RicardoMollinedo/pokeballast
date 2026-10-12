package com.ballastlane.pokeproject.api;

import com.ballastlane.pokeproject.bl.AuthBl;
import com.ballastlane.pokeproject.dto.AuthResponse;
import com.ballastlane.pokeproject.dto.CredentialsRequest;
import com.ballastlane.pokeproject.dto.UserResponse;
import com.ballastlane.pokeproject.config.SecurityConfig;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import com.ballastlane.pokeproject.util.LogSanitizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, LogSanitizer.class})
class AuthControllerTest {

    private static final UserResponse MISTY = new UserResponse("misty", "TRAINER");

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AuthBl authBl;

    @Test
    void registerIsPublicAndReturns201() throws Exception {
        when(authBl.register(new CredentialsRequest("Misty", "Starmie123"))).thenReturn(MISTY);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Misty\",\"password\":\"Starmie123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.username").value("misty"))
                .andExpect(jsonPath("$.response.role").value("TRAINER"));
    }

    @Test
    void registerWithMissingFieldsIs400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1000"))
                .andExpect(jsonPath("$.response.username").exists())
                .andExpect(jsonPath("$.response.password").exists());
    }

    @Test
    void weakPasswordIs400WithTheBusinessMessage() throws Exception {
        when(authBl.register(any())).thenThrow(new BallastException(BallastErrorCode.INVALID_USER_DATA,
                "Invalid username or password format", Map.of("password", "must contain at least one letter and one digit")));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"misty\",\"password\":\"onlyletters\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1007"))
                .andExpect(jsonPath("$.response.password").exists());
    }

    @Test
    void takenUsernameIs409() throws Exception {
        when(authBl.register(any())).thenThrow(new BallastException(BallastErrorCode.USERNAME_TAKEN));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ash\",\"password\":\"Pikachu123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseCode").value("3004"));
    }

    @Test
    void loginIsPublicAndReturnsABearerToken() throws Exception {
        when(authBl.login(new CredentialsRequest("misty", "Starmie123"))).thenReturn(new AuthResponse(
                "header.payload.signature", "Bearer", Instant.parse("2030-01-01T00:00:00Z"), MISTY));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"misty\",\"password\":\"Starmie123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.response.accessToken").value("header.payload.signature"))
                .andExpect(jsonPath("$.response.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.response.expiresAt").value("2030-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.response.user.role").value("TRAINER"));
    }

    @Test
    void badCredentialsAre401() throws Exception {
        when(authBl.login(any())).thenThrow(new BallastException(BallastErrorCode.INVALID_CREDENTIALS));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"misty\",\"password\":\"wrong-pass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4002"))
                .andExpect(jsonPath("$.errorDetail").value("Invalid username or password"));
    }

    @Test
    void meNeedsAToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.responseCode").value("4001"));
    }

    @Test
    void meReadsTheUserFromTheToken() throws Exception {
        when(authBl.currentUser("admin", "ADMIN")).thenReturn(new UserResponse("admin", "ADMIN"));

        mvc.perform(get("/api/v1/auth/me").with(jwt().jwt(j -> j.subject("admin").claim("roles", List.of("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.username").value("admin"))
                .andExpect(jsonPath("$.response.role").value("ADMIN"));
    }
}
