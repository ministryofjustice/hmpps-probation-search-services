package uk.gov.justice.digital.hmpps.probation.search

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [ElasticsearchDataAutoConfiguration::class])
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}