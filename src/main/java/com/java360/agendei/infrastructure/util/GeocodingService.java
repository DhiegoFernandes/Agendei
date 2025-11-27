package com.java360.agendei.infrastructure.util;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.java360.agendei.infrastructure.dto.LatLngDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final GeoApiContext context;

    // Recebe CEP, endereço completo ou qualquer string de endereço válida.
    public LatLngDTO buscarLatLong(String enderecoCompleto) {
        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, enderecoCompleto).await();

            if (results.length > 0) {
                double lat = results[0].geometry.location.lat;
                double lng = results[0].geometry.location.lng;

                return new LatLngDTO(lat, lng);
            }

            System.out.println("Não foi possível encontrar coordenadas para: " + enderecoCompleto);
            return null;

        } catch (Exception e) {
            System.out.println("Erro ao consultar Geocoding API: " + e.getMessage());
            return null;
        }
    }

    // Compatibilidade com antigo metodo (apenas CEP)
    public LatLngDTO buscarLatLongPorCep(String cep) {
        return buscarLatLong(cep);
    }
}
