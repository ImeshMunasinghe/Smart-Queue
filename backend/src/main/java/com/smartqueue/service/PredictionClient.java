package com.smartqueue.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class PredictionClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionClient.class);
    private final RestTemplate restTemplate;

    @Value("${prediction.service.url:http://localhost:8000}")
    private String predictionServiceUrl;

    public PredictionClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(800))
                .setReadTimeout(Duration.ofMillis(1200))
                .build();
    }

    public int predictWaitTimeSeconds(UUID officeId, UUID serviceTypeId, int queueDepth, int defaultDurationSeconds) {
        try {
            String url = predictionServiceUrl + "/predict/wait-time";
            int hourOfDay = java.time.LocalTime.now().getHour();
            int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue();

            Map<String, Object> payload = Map.of(
                    "office_id", officeId.toString(),
                    "service_type_id", serviceTypeId.toString(),
                    "hour_of_day", hourOfDay,
                    "day_of_week", dayOfWeek,
                    "queue_depth", queueDepth,
                    "default_duration_seconds", defaultDurationSeconds
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object waitSec = response.getBody().get("predicted_wait_seconds");
                if (waitSec instanceof Number num) {
                    return num.intValue();
                }
            }
        } catch (Exception e) {
            log.warn("Prediction microservice unavailable ({}), falling back to heuristic calculation", e.getMessage());
        }

        // Resilient Heuristic Fallback (FR-8.4)
        return Math.max(1, queueDepth) * defaultDurationSeconds;
    }
}
