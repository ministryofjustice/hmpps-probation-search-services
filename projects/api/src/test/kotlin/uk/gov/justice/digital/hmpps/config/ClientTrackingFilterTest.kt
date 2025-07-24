package uk.gov.justice.digital.hmpps.config

import io.opentelemetry.api.trace.Span
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@ExtendWith(MockitoExtension::class)
@AutoConfigureMockMvc(addFilters = true)
class ClientTrackingFilterTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should set clientId attribute on current span`() {
        mockStatic(Span::class.java).use { static ->
            val span = mock(Span::class.java)
            static.`when`<Span> { Span.current() }.thenReturn(span)

            mockMvc.post("/test") {
                with(jwt().jwt { it.claim("client_id", "test-client-id") })
            }

            verify(span).setAttribute("clientId", "test-client-id")
        }
    }
}