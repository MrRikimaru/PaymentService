package com.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomNumberService {

    @Value("${external.api.random-number.url}")
    private String randomNumberApiUrl;

    private final WebClient webClient;

    public Mono<Integer> getRandomNumber() {
        log.debug("Calling external API: {}", randomNumberApiUrl);
        return webClient.get()
                .uri(randomNumberApiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(String::trim)
                .map(Integer::parseInt)
                .doOnSuccess(number -> log.debug("Random number from API: {}", number))
                .doOnError(error -> log.error("Error calling external API for random number: {}", error.getMessage()));
    }
}