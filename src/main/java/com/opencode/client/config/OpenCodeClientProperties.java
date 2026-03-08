package com.opencode.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the OpenCode client.
 *
 * <p>In a Spring Boot application, these can be set via {@code application.yml}:
 * <pre>
 * opencode:
 *   client:
 *     base-url: http://localhost:4096
 *     username: opencode
 *     password: secret
 *     connect-timeout: 5s
 *     response-timeout: 60s
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "opencode.client")
public class OpenCodeClientProperties {

    /** Base URL of the OpenCode server. Default: http://localhost:4096 */
    private String baseUrl = "http://localhost:4096";

    /** Basic auth username (optional). */
    private String username;

    /** Basic auth password (optional). */
    private String password;

    /** HTTP connect timeout. Default: 10 seconds. */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** HTTP response timeout. Default: 120 seconds. */
    private Duration responseTimeout = Duration.ofSeconds(120);

    /** Max retries on transient failures. Default: 0 (no retry). */
    private int maxRetries = 0;
}
