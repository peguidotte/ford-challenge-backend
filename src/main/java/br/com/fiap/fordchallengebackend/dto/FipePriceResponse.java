package br.com.fiap.fordchallengebackend.dto;

public record FipePriceResponse(
    String brand,
    String model,
    Integer modelYear,
    String fuel,
    String codeFipe,
    String price,
    String referenceMonth
) {
}
