package uk.gov.justice.digital.hmpps.probation.search.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.time.Duration
import java.time.Instant

@Configuration
class TokenHelper {
    companion object {
        const val TOKEN = "token"
    }

    @Bean
    fun jwt(): Jwt = Jwt
        .withTokenValue(TOKEN)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plus(Duration.ofDays(1)))
        .header("alg", "none")
        .claim("sub", "dev")
        .claim("authorities", listOf("ROLE_PROBATION_SEARCH__PERSON"))
        .build()

    @Bean
    @Primary
    fun jwtDecoder(jwt: Jwt) = JwtDecoder { jwt }
}
