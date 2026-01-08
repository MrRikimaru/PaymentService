package com.example.paymentservice.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RandomNumberServiceTest {

    @Mock private WebClient webClient;

    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock private WebClient.ResponseSpec responseSpec;

    @InjectMocks private RandomNumberService randomNumberService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                randomNumberService,
                "randomNumberApiUrl",
                "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");

        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void getRandomNumber_ShouldReturnInteger_WhenApiCallSucceeds() {
        // Arrange
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("42"));

        // Act & Assert
        StepVerifier.create(randomNumberService.getRandomNumber()).expectNext(42).verifyComplete();

        // Verify interactions
        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(anyString());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void getRandomNumber_ShouldHandleError_WhenApiCallFails() {
        // Arrange
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        // Act & Assert
        StepVerifier.create(randomNumberService.getRandomNumber())
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(anyString());
        verify(requestHeadersSpec).retrieve();
    }

    @Test
    void getRandomNumber_ShouldParseInteger_WhenResponseHasWhitespace() {
        // Arrange
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("  42  "));

        // Act & Assert
        StepVerifier.create(randomNumberService.getRandomNumber()).expectNext(42).verifyComplete();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(anyString());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(String.class);
    }
}
