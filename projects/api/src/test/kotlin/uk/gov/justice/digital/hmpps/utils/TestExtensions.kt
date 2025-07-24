package uk.gov.justice.digital.hmpps.utils

import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
import org.springframework.test.web.servlet.MockHttpServletRequestDsl

object TestExtensions {
    fun JwtRequestPostProcessor.roles(vararg roles: String): JwtRequestPostProcessor =
        authorities(roles.map { SimpleGrantedAuthority("ROLE_$it") })

    var MockHttpServletRequestDsl.json: String?
        get() = null
        set(body) {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }
}
