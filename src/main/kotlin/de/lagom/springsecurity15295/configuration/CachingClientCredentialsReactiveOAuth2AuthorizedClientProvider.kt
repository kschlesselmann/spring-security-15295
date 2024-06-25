package de.lagom.springsecurity15295.configuration

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthorizationException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

class CachingClientCredentialsReactiveOAuth2AuthorizedClientProvider : ReactiveOAuth2AuthorizedClientProvider {

    private val accessTokenClient: ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> =
        WebClientReactiveClientCredentialsTokenResponseClient()

    private val cache: AsyncLoadingCache<ClientRegistration, OAuth2AccessToken> = Caffeine.newBuilder()
        .expireAfter(OAuth2AccessTokenExpiry())
        .buildAsync { clientRegistration, _ -> getAccessToken(clientRegistration) }

    override fun authorize(context: OAuth2AuthorizationContext): Mono<OAuth2AuthorizedClient> {
        val clientRegistration: ClientRegistration = context.clientRegistration

        if (clientRegistration.authorizationGrantType != AuthorizationGrantType.CLIENT_CREDENTIALS) {
            return Mono.empty()
        }

        return Mono.fromFuture { cache.get(clientRegistration) }
            .map { OAuth2AuthorizedClient(clientRegistration, context.principal.name, it) }
    }

    private fun getAccessToken(clientRegistration: ClientRegistration): CompletableFuture<OAuth2AccessToken> =
        OAuth2ClientCredentialsGrantRequest(clientRegistration).toMono()
            .flatMap { accessTokenClient.getTokenResponse(it) }
            .onErrorMap(OAuth2AuthorizationException::class.java) {
                ClientAuthorizationException(it.error, clientRegistration.registrationId, it)
            }
            .map { it.accessToken }
            .toFuture()
}

private class OAuth2AccessTokenExpiry : Expiry<ClientRegistration, OAuth2AccessToken> {

    override fun expireAfterCreate(
        clientRegistration: ClientRegistration,
        token: OAuth2AccessToken,
        currentTimeInNanos: Long,
    ): Long = token.cacheDurationInNanos()

    override fun expireAfterUpdate(
        clientRegistration: ClientRegistration,
        token: OAuth2AccessToken,
        currentTimeInNanos: Long,
        currentDurationInNanos: Long,
    ): Long = token.cacheDurationInNanos()

    override fun expireAfterRead(
        clientRegistration: ClientRegistration,
        token: OAuth2AccessToken,
        currentTimeInNanos: Long,
        currentDurationInNanos: Long,
    ): Long = token.cacheDurationInNanos()
}

private fun OAuth2AccessToken.cacheDurationInNanos(): Long =
    Duration.between(Instant.now(), expiresAt)
        .minus(Duration.ofMinutes(1))
        .toNanos()
