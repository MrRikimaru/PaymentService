package com.example.paymentservice.integration;

import com.example.paymentservice.PaymentServiceApplication;
import com.example.paymentservice.dto.PaymentRequestDTO;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = PaymentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:6.0"))
            .withExposedPorts(27017);

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();

        WireMock.configureFor("localhost", 8089);
        configureWireMock();
    }

    @AfterAll
    static void afterAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        wireMockServer.resetAll();
        configureWireMock();
    }

    static void configureWireMock() {
        // Default stub for random number API
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // MongoDB properties
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test_payments");

        // WireMock properties
        registry.add("external.api.random-number.url",
                () -> "http://localhost:8089/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new");

        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");

        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");

        // Liquibase
        registry.add("spring.liquibase.enabled", () -> false);

        // Disable actuator endpoints for tests
        registry.add("management.endpoints.web.exposure.include", () -> "");
    }

    @Test
    void createPayment_ShouldReturnSuccess_WhenExternalApiReturnsEvenNumber() throws Exception {
        // Arrange - Mock external API response (even number = 42)
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("42")));

        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(1001L)
                .userId(501L)
                .amount(new BigDecimal("99.99"))
                .build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.userId").value(501))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentAmount").value(99.99));

        // Verify data is persisted
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void createPayment_ShouldReturnFailed_WhenExternalApiReturnsOddNumber() throws Exception {
        // Arrange - Mock external API response (odd number = 41)
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("41")));

        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(1002L)
                .userId(502L)
                .amount(new BigDecimal("50.00"))
                .build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1002))
                .andExpect(jsonPath("$.status").value("FAILED"));

        // Verify data is persisted
        List<Payment> payments = paymentRepository.findByOrderId(1002L);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void createPayment_ShouldReturnFailed_WhenExternalApiIsUnavailable() throws Exception {
        // Arrange - Mock external API timeout
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new"))
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(5000) // Simulate timeout
                        .withStatus(500)));

        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(1003L)
                .userId(503L)
                .amount(new BigDecimal("75.50"))
                .build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1003))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getPayments_ShouldReturnPayments_WithFilters() throws Exception {
        // Arrange - Create test data
        Payment payment1 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(2001L)
                .userId(601L)
                .status("SUCCESS")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        Payment payment2 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(2001L)
                .userId(601L)
                .status("FAILED")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        Payment payment3 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(2002L)
                .userId(602L)
                .status("SUCCESS")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        // Act & Assert - Get all payments
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // Act & Assert - Filter by userId
        mockMvc.perform(get("/api/payments")
                        .param("userId", "601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Act & Assert - Filter by orderId
        mockMvc.perform(get("/api/payments")
                        .param("orderId", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Act & Assert - Filter by status
        mockMvc.perform(get("/api/payments")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Act & Assert - Filter by userId and status
        mockMvc.perform(get("/api/payments")
                        .param("userId", "601")
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paymentAmount").value(100.00));
    }

    @Test
    void getTotalForUser_ShouldCalculateCorrectTotalForDateRange() throws Exception {
        // Arrange - Create test data with timestamps truncated to milliseconds
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

        Payment payment1 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(3001L)
                .userId(701L)
                .status("SUCCESS")
                .timestamp(twoHoursAgo) // Definitely outside range
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        Payment payment2 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(3002L)
                .userId(701L)
                .status("SUCCESS")
                .timestamp(oneHourAgo) // Inside range
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        Payment payment3 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(3003L)
                .userId(701L)
                .status("SUCCESS")
                .timestamp(now) // Inside range
                .paymentAmount(new BigDecimal("300.00"))
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        // Wait for persistence
        Thread.sleep(200);

        // Use the same truncated timestamps for query
        Instant queryStart = oneHourAgo;
        Instant queryEnd = now;

        // Act & Assert
        mockMvc.perform(get("/api/payments/user/{userId}/total", 701L)
                        .param("startDate", queryStart.toString())
                        .param("endDate", queryEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getTotalForAllUsers_ShouldCalculateCorrectTotalForDateRange() throws Exception {
        // Arrange
        Instant startDate = Instant.now().minusSeconds(7200);
        Instant endDate = Instant.now();

        Payment payment1 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(4001L)
                .userId(801L)
                .status("SUCCESS")
                .timestamp(Instant.now().minusSeconds(3600))
                .paymentAmount(new BigDecimal("150.00"))
                .build();

        Payment payment2 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(4002L)
                .userId(802L)
                .status("SUCCESS")
                .timestamp(Instant.now().minusSeconds(1800))
                .paymentAmount(new BigDecimal("250.00"))
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2));

        // Act & Assert
        mockMvc.perform(get("/api/payments/admin/total")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(400.00))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getPaymentsByUserId_ShouldReturnUserPayments() throws Exception {
        // Arrange
        Payment payment1 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(5001L)
                .userId(901L)
                .status("SUCCESS")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        Payment payment2 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(5002L)
                .userId(901L)
                .status("FAILED")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        Payment payment3 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(5003L)
                .userId(902L)
                .status("SUCCESS")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2, payment3));

        // Act & Assert
        mockMvc.perform(get("/api/payments/user/{userId}", 901L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getPaymentsByOrderId_ShouldReturnOrderPayments() throws Exception {
        // Arrange
        Payment payment1 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(6001L)
                .userId(1001L)
                .status("SUCCESS")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        Payment payment2 = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(6001L)
                .userId(1001L)
                .status("FAILED")
                .timestamp(Instant.now())
                .paymentAmount(new BigDecimal("50.00"))
                .build();

        paymentRepository.saveAll(List.of(payment1, payment2));

        // Act & Assert
        mockMvc.perform(get("/api/payments/order/{orderId}", 6001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void createPayment_ShouldReturnBadRequest_WhenInvalidInput() throws Exception {
        // Arrange - Invalid request (negative amount)
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(1001L)
                .userId(501L)
                .amount(new BigDecimal("-10.00")) // Invalid amount
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void createPayment_ShouldReturnBadRequest_WhenMissingRequiredFields() throws Exception {
        // Arrange - Missing userId
        String invalidJson = """
            {
                "orderId": 1001,
                "amount": 100.00
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").exists());
    }
}