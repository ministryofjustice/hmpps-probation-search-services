package uk.gov.justice.digital.hmpps.probation.search

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration

@Configuration
class DataLoader(private val entityManager: EntityManager) : ApplicationListener<ApplicationReadyEvent> {
    @Transactional
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
    }
}
