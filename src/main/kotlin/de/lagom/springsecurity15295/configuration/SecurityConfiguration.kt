package de.lagom.springsecurity15295.configuration

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    @Profile("!cache")
    fun defaultAuthorizedClientProvider(): ReactiveOAuth2AuthorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()

    @Bean
    @Profile("cache")
    fun cachingAuthorizedClientProvider(): ReactiveOAuth2AuthorizedClientProvider =
        CachingClientCredentialsReactiveOAuth2AuthorizedClientProvider()

    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ReactiveClientRegistrationRepository,
        authorizedClientRepository: ServerOAuth2AuthorizedClientRepository,
        authorizedClientService: ReactiveOAuth2AuthorizedClientService,
        reactiveOAuth2AuthorizedClientProvider: ReactiveOAuth2AuthorizedClientProvider,
    ): ReactiveOAuth2AuthorizedClientManager {
        return DefaultReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientRepository,
        )

//        return AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
//            clientRegistrationRepository,
//            authorizedClientService,
//        )
//            .apply {
//                setAuthorizedClientProvider(reactiveOAuth2AuthorizedClientProvider)
//            }
    }

    @Bean
    fun customizer(authorizedClientManager: ReactiveOAuth2AuthorizedClientManager): WebClientCustomizer =
        WebClientCustomizer {
            it.filter(ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager))
        }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .cors { }
            .csrf { it.disable() }
            .authorizeExchange {
                it
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt { }
            }
            .build()
}

