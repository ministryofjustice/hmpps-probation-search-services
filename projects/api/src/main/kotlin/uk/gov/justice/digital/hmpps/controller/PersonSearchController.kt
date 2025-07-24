package uk.gov.justice.digital.hmpps.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.model.PersonSearchRequest
import uk.gov.justice.digital.hmpps.model.PersonSearchResult

@RestController
@Tag(name = "Person")
@PreAuthorize("hasRole('PROBATION_SEARCH__PERSON')")
class PersonSearchController {
    @Operation(
        summary = "Search for a person on probation",
        description = """
            The query can contain any of the following:
              
              - Names
              - Aliases
              - Addresses
              - Date of birth (in any common format)
              - Identifiers (CRN, NOMIS ID, PNC ID, CRO ID, National Insurance number)
              - Gender
              - Telephone numbers
              
            Client must have ROLE_PROBATION__SEARCH_PERSON
            """
    )
    @PostMapping("/people")
    fun search(
        @Valid @RequestBody request: PersonSearchRequest,
        @ParameterObject @PageableDefault pageable: Pageable,
    ) = PagedModel<PersonSearchResult>(Page.empty())
}