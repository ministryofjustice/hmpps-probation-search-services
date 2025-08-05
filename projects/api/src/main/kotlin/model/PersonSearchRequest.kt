package uk.gov.justice.digital.hmpps.probation.search.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PersonSearchRequest(
    @NotBlank(message = "Query must be supplied")
    @Size(max = 1000, message = "Query must be no longer than 1000 characters")
    @Schema(
        required = true,
        description = "Phrase containing the terms to search for",
        example = "john smith 19/07/1965"
    )
    val query: String,

    @Schema(
        required = false,
        description = "When true, only return cases that match all tokens in the query. Analogous to AND versus OR"
    )
    val matchAllTerms: Boolean = false,

    @Schema(
        required = false,
        description = "Filter on provider codes. Only cases that have an active manager in one of the areas will be returned. Leave blank to return cases in all provider.",
        example = "[\"N01\",\"N02\"]"
    )
    val providers: List<String> = listOf(),
)