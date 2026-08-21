package com.allobank.backendtestallobank.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.allobank.backendtestallobank.dto.auth.LoginRequest;
import com.allobank.backendtestallobank.dto.auth.LoginResponse;
import com.allobank.backendtestallobank.entity.user.UserEntity;
import com.allobank.backendtestallobank.repository.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtEncoder jwtEncoder;

	@InjectMocks
	private AuthService authService;

	@Test
	void loginCreatesUserAndEncodesJwtWhenEmailIsNew() {
		when(userRepository.findByEmail("arif@example.com")).thenReturn(Optional.empty());
		when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> jwtFromClaims(
				"new-user-token",
				invocation.getArgument(0)));

		LoginResponse response = authService.login(new LoginRequest(" Arif Rahman ", " ARIF@EXAMPLE.COM "));

		ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
		verify(userRepository).save(userCaptor.capture());
		UserEntity savedUser = userCaptor.getValue();
		assertThat(savedUser.getId()).isNotNull();
		assertThat(savedUser.getFullName()).isEqualTo("Arif Rahman");
		assertThat(savedUser.getEmail()).isEqualTo("arif@example.com");

		ArgumentCaptor<JwtEncoderParameters> tokenCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
		verify(jwtEncoder).encode(tokenCaptor.capture());
		JwtClaimsSet claims = tokenCaptor.getValue().getClaims();
		assertThat(claims.getSubject()).isEqualTo(savedUser.getId().toString());
		assertThat(claims.getClaimAsString("email")).isEqualTo("arif@example.com");
		assertThat(claims.getClaimAsString("fullName")).isEqualTo("Arif Rahman");
		assertThat(Duration.between(claims.getIssuedAt(), claims.getExpiresAt())).isEqualTo(Duration.ofHours(24));

		assertThat(response.accessToken()).isEqualTo("new-user-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresAt()).isEqualTo(claims.getExpiresAt());
		assertThat(response.user().id()).isEqualTo(savedUser.getId());
		assertThat(response.user().fullName()).isEqualTo("Arif Rahman");
		assertThat(response.user().email()).isEqualTo("arif@example.com");
	}

	@Test
	void loginUsesExistingUserWhenEmailAlreadyExists() {
		UserEntity existingUser = new UserEntity(USER_ID, "Existing User", "arif@example.com");
		when(userRepository.findByEmail("arif@example.com")).thenReturn(Optional.of(existingUser));
		when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> jwtFromClaims(
				"existing-user-token",
				invocation.getArgument(0)));

		LoginResponse response = authService.login(new LoginRequest("Arif Rahman", "arif@example.com"));

		verify(userRepository, never()).save(any(UserEntity.class));
		ArgumentCaptor<JwtEncoderParameters> tokenCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
		verify(jwtEncoder).encode(tokenCaptor.capture());
		assertThat(tokenCaptor.getValue().getClaims().getSubject()).isEqualTo(USER_ID.toString());

		assertThat(response.accessToken()).isEqualTo("existing-user-token");
		assertThat(response.user().id()).isEqualTo(USER_ID);
		assertThat(response.user().fullName()).isEqualTo("Existing User");
		assertThat(response.user().email()).isEqualTo("arif@example.com");
	}

	private Jwt jwtFromClaims(String tokenValue, JwtEncoderParameters parameters) {
		return Jwt.withTokenValue(tokenValue)
				.header("alg", "HS256")
				.claims(claims -> claims.putAll(parameters.getClaims().getClaims()))
				.build();
	}
}