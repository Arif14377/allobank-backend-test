package com.allobank.backendtestallobank.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.allobank.backendtestallobank.service.auth.AuthService;
import com.allobank.backendtestallobank.common.error.GlobalExceptionHandler;
import com.allobank.backendtestallobank.config.security.SecurityConfig;
import com.allobank.backendtestallobank.dto.auth.AuthenticatedUserResponse;
import com.allobank.backendtestallobank.dto.auth.LoginRequest;
import com.allobank.backendtestallobank.dto.auth.LoginResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
@ImportAutoConfiguration({ SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class })
class AuthControllerTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private JwtEncoder jwtEncoder;

	@Test
	void loginReturnsJwtWithoutBearerToken() throws Exception {
		LoginResponse response = new LoginResponse(
				"jwt-token",
				"Bearer",
				Instant.parse("2026-08-20T14:00:00Z"),
				new AuthenticatedUserResponse(USER_ID, "Arif Rahman", "arif@example.com"));
		when(authService.login(any(LoginRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "fullName": "Arif Rahman",
							  "email": "arif@example.com"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("jwt-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresAt").value("2026-08-20T14:00:00Z"))
				.andExpect(jsonPath("$.user.id").value(USER_ID.toString()))
				.andExpect(jsonPath("$.user.fullName").value("Arif Rahman"))
				.andExpect(jsonPath("$.user.email").value("arif@example.com"));

		verify(authService).login(any(LoginRequest.class));
	}

	@Test
	void loginRejectsInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "fullName": " ",
							  "email": "not-an-email"
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Request validation failed"));

		verifyNoInteractions(authService);
	}
}