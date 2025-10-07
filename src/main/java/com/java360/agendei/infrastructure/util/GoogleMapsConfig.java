package com.java360.agendei.infrastructure.util;

import com.google.maps.GeoApiContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleMapsConfig {

    @Bean
    public GeoApiContext geoApiContext() {
        return new GeoApiContext.Builder()
                .apiKey("AIzaSyDQHCfEBOf_EO6Abo4Q-n987llQhru87Rw")
                .build();
    }
}
