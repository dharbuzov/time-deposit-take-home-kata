package org.ikigaidigital.application

import org.ikigaidigital.domain.TimeDepositCalculator
import org.ikigaidigital.domain.interest.InterestPolicyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ApplicationConfiguration {
    @Bean
    fun timeDepositCalculator(): TimeDepositCalculator =
        TimeDepositCalculator()

    @Bean
    fun interestPolicyResolver(): InterestPolicyResolver =
        InterestPolicyResolver()

    @Bean
    fun clock(): Clock =
        Clock.systemDefaultZone()
}
