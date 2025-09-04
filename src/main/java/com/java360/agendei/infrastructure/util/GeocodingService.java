package com.java360.agendei.infrastructure.util;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.java360.agendei.infrastructure.dto.LatLngDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final GeoApiContext context;

    public LatLngDTO buscarLatLongPorCep(String cep) {
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, cep).await();
            if (results.length > 0) {
                double lat = results[0].geometry.location.lat;
                double lng = results[0].geometry.location.lng;
                return new LatLngDTO(lat, lng);
            }
            throw new IllegalArgumentException("Não foi possível encontrar coordenadas para o CEP: " + cep);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar Geocoding API: " + e.getMessage(), e);
        }
    }
}

