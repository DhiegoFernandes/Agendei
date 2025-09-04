package com.java360.agendei.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class LatLngDTO {
    private double lat;
    private double lng;
}
