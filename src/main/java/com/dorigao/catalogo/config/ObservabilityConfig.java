package com.dorigao.catalogo.config;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public HealthIndicator customHealthIndicator() {
        return () -> Health.up()
                .withDetail("service", "catalogo")
                .withDetail("version", "1.0.0")
                .build();
    }

    @Bean
    public MeterFilter commonTagsFilter() {
        return MeterFilter.commonTags(
                Tags.of("application", "catalogo",
                        "service.name", "catalogo",
                        "service.namespace", "app",
                        "service_name", "catalogo")
        );
    }
}
