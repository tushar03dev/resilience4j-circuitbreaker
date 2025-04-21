package com.example.servicea.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequestMapping("/activity")
@RestController
public class ActivityController {

    private final RestTemplate restTemplate;

    @Value("${joke.api.url}")
    private String API;

    public ActivityController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    @CircuitBreaker(name = "randomActivity", fallbackMethod = "fallbackRandomActivity")
    public String getRandomActivity() {
        try {
            ResponseEntity<JsonNode> responseEntity = restTemplate.getForEntity(API, JsonNode.class);
            JsonNode body = responseEntity.getBody();

            if (body == null || body.path("error").asBoolean()) {
                throw new RuntimeException("Invalid or error response from API");
            }

            log.info("Full Joke Response: {}", body.toPrettyString());

            String type = body.path("type").asText();

            switch (type) {
                case "twopart":
                    return body.path("setup").asText() + " " + body.path("delivery").asText();

                case "single":
                    return body.path("joke").asText();

                default:
                    return "Unknown joke format.";
            }
        } catch (Exception e) {
            log.error("Error while fetching activity: {}", e.getMessage());
            throw e; // Rethrow to trigger fallback
        }
    }

    public String fallbackRandomActivity(Throwable throwable) {
        log.error("Fallback triggered due to: {}", throwable.getClass().getName() + ": " + throwable.getMessage());
        // Log the full exception stack trace for debugging
        throwable.printStackTrace();
        return "My boss said “dress for the job you want, not for the job you have.” So I went in as Batman.";
    }


}
