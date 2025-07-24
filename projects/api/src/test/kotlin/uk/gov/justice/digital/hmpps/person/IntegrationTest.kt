package uk.gov.justice.digital.hmpps.person

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import uk.gov.justice.digital.hmpps.model.PersonSearchRequest
import uk.gov.justice.digital.hmpps.utils.TestExtensions.json
import uk.gov.justice.digital.hmpps.utils.TestExtensions.roles

@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `calls the api`() {
        mockMvc.post("/people") {
            with(jwt().roles("PROBATION_SEARCH__PERSON"))
            json = objectMapper.writeValueAsString(PersonSearchRequest(query = "test"))
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.content").isEmpty
            }
        }
    }
}