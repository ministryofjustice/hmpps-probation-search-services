package uk.gov.justice.digital.hmpps.probation.search.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.probation.search.TestExtensions.json
import uk.gov.justice.digital.hmpps.probation.search.TestExtensions.roles
import uk.gov.justice.digital.hmpps.probation.search.model.PersonSearchRequest

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `no token returns 401`() {
        mockMvc.post("/people").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `missing role returns 403`() {
        mockMvc.post("/people") {
            with(jwt().roles("ANOTHER_ROLE"))
            json = objectMapper.writeValueAsString(PersonSearchRequest(query = "test"))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `correct role returns 200`() {
        mockMvc.post("/people") {
            with(jwt().roles("PROBATION_SEARCH__PERSON"))
            json = objectMapper.writeValueAsString(PersonSearchRequest(query = "test"))
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `no auth required for health`() {
        mockMvc.get("/health").andExpect { status { isOk() } }
    }

    @Test
    fun `no auth required for info`() {
        mockMvc.get("/info").andExpect { status { isOk() } }
    }

    @Test
    fun `no auth required for docs`() {
        mockMvc.get("/swagger-ui/index.html").andExpect { status { isOk() } }
    }
}