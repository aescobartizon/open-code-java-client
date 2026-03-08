package com.opencode.client.config;

import com.opencode.client.OpenCodeClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Spring Boot auto-configuration for the OpenCode client.
 *
 * <p>Automatically creates an {@link OpenCodeClient} bean if none is defined.
 * Activated when {@link WebClient} is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(OpenCodeClientProperties.class)
public class OpenCodeClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenCodeClient openCodeClient(OpenCodeClientProperties properties) {
        return new OpenCodeClient(buildWebClient(properties), properties);
    }

    private WebClient buildWebClient(OpenCodeClientProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) props.getConnectTimeout().toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                props.getResponseTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                props.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)));

        // Increase codec buffer limit to 16 MB for large responses (e.g. GET /provider)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        // Optional Basic Auth
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            String user = props.getUsername() != null ? props.getUsername() : "opencode";
            String credentials = Base64.getEncoder()
                    .encodeToString((user + ":" + props.getPassword()).getBytes());
            builder.defaultHeader("Authorization", "Basic " + credentials);
        }

        return builder.build();
    }
}
