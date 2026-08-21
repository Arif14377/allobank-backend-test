package com.allobank.backendtestallobank.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.allobank.backendtestallobank.dto.auth.AuthenticatedUserResponse;
import com.allobank.backendtestallobank.dto.auth.LoginRequest;
import com.allobank.backendtestallobank.dto.auth.LoginResponse;
import com.allobank.backendtestallobank.entity.user.UserEntity;
import com.allobank.backendtestallobank.repository.user.UserRepository;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Duration TOKEN_TTL = Duration.ofHours(24);
	private static final String TOKEN_TYPE = "Bearer";

	private final UserRepository userRepository;
	private final JwtEncoder jwtEncoder;

	public AuthService(UserRepository userRepository, JwtEncoder jwtEncoder) {
		this.userRepository = userRepository;
		this.jwtEncoder = jwtEncoder;
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		String fullName = request.fullName().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		UserEntity user = userRepository.findByEmail(email)
				.orElseGet(() -> userRepository.save(new UserEntity(UUID.randomUUID(), fullName, email)));

		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(TOKEN_TTL);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("email", user.getEmail())
				.claim("fullName", user.getFullName())
				.build();
		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
		Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));

		return new LoginResponse(
				jwt.getTokenValue(),
				TOKEN_TYPE,
				jwt.getExpiresAt(),
				new AuthenticatedUserResponse(user.getId(), user.getFullName(), user.getEmail()));
	}
}