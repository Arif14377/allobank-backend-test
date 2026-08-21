package com.allobank.backendtestallobank.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

class SecurityConfigTest {

	@Test
	void jwtEncoderTokenCanBeDecodedByJwtDecoder() {
		SecurityConfig securityConfig = new SecurityConfig();
		String secret = "test-only-32-byte-minimum-secret-value";
		JwtEncoder jwtEncoder = securityConfig.jwtEncoder(secret);
		JwtDecoder jwtDecoder = securityConfig.jwtDecoder(secret);
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject("00000000-0000-0000-0000-000000000001")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(3600))
				.claim("email", "arif@example.com")
				.claim("fullName", "Arif Rahman")
				.build();
		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();

		Jwt encoded = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));
		Jwt decoded = jwtDecoder.decode(encoded.getTokenValue());

		assertThat(decoded.getSubject()).isEqualTo("00000000-0000-0000-0000-000000000001");
		assertThat(decoded.getClaimAsString("email")).isEqualTo("arif@example.com");
		assertThat(decoded.getClaimAsString("fullName")).isEqualTo("Arif Rahman");
	}
}