package br.com.fiap.fordchallengebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FipePriceResponse(
    @JsonProperty("brand") String brand,
    @JsonProperty("model") String model,
    @JsonProperty("modelYear") Integer modelYear,
    @JsonProperty("fuel") String fuel,
    @JsonProperty("codeFipe") String codeFipe,
    @JsonProperty("price") String price,
    @JsonProperty("referenceMonth") String referenceMonth
) {
}
