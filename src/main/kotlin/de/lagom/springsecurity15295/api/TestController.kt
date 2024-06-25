package de.lagom.springsecurity15295.api

import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux

data class Thing(
    val name: String,
)

@RestController
@RequestMapping("/things")
class TestController(
    webClientBuilder: WebClient.Builder
) {

    val client: WebClient = webClientBuilder
        .baseUrl("http://localhost:8080")
        .build()

    @GetMapping
    fun findAll(): Flux<Thing> =
        Flux.range(1, 100)
            .map { Thing(name = "Thing $it") }

    @PostMapping
    fun trigger() =
        Flux.range(1, 100)
            .flatMap {
                client.get()
                    .uri("/things")
                    .accept(MediaType.APPLICATION_NDJSON)
                    .attributes(clientRegistrationId("test"))
                    .retrieve()
                    .bodyToFlux<Thing>()
            }
            .count()
            .doOnNext { println("Retrieved $it things") }
}