package org.ikigaidigital.application

import org.ikigaidigital.domain.TimeDepositCalculator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfiguration {
    @Bean
    fun timeDepositCalculator(): TimeDepositCalculator =
        TimeDepositCalculator()
}
