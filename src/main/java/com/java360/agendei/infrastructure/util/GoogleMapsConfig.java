package com.java360.agendei.infrastructure.util;

import com.google.maps.GeoApiContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleMapsConfig {

    @Bean
    public GeoApiContext geoApiContext() {
        return new GeoApiContext.Builder()
                .apiKey("AIzaSyByUCkvwfPoKnpQ7ufJJ6T-AapUJBTQvfA")
                .build();
    }
}
