package br.com.fiap.fordchallengebackend.service;

import br.com.fiap.fordchallengebackend.domain.VehicleQueryAttributeResult;
import br.com.fiap.fordchallengebackend.domain.VehicleQueryHistory;
import br.com.fiap.fordchallengebackend.dto.FipeBrandResponse;
import br.com.fiap.fordchallengebackend.dto.FipeModelResponse;
import br.com.fiap.fordchallengebackend.dto.VehicleQueryRequest;
import br.com.fiap.fordchallengebackend.dto.VehicleQueryResponse;
import br.com.fiap.fordchallengebackend.exception.ExternalIntegrationException;
import br.com.fiap.fordchallengebackend.integration.carapi.CarApiClient;
import br.com.fiap.fordchallengebackend.integration.fipe.FipeClient;
import br.com.fiap.fordchallengebackend.repository.VehicleEnrichmentOverrideRepository;
import br.com.fiap.fordchallengebackend.repository.VehicleQueryHistoryRepository;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleSpecificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VehicleSpecificationService.class);
    private static final String NOT_AVAILABLE = "Nao disponivel";
    private static final String DEFAULT_VEHICLE_TYPE = "cars";

    private final CarApiClient carApiClient;
    private final FipeClient fipeClient;
    private final VehicleEnrichmentOverrideRepository enrichmentOverrideRepository;
    private final VehicleQueryHistoryRepository queryHistoryRepository;

    public VehicleSpecificationService(
        CarApiClient carApiClient,
        FipeClient fipeClient,
        VehicleEnrichmentOverrideRepository enrichmentOverrideRepository,
        VehicleQueryHistoryRepository queryHistoryRepository
    ) {
        this.carApiClient = carApiClient;
        this.fipeClient = fipeClient;
        this.enrichmentOverrideRepository = enrichmentOverrideRepository;
        this.queryHistoryRepository = queryHistoryRepository;
    }

    @Transactional
    public VehicleQueryResponse query(VehicleQueryRequest request) {
        var requestedAttributes = normalizeRequestedAttributes(request.attributes());

        var carApiData = safeCarApiLookup(request);
        var fipeData = safeFipeLookup(request);

        var normalizedCarApiData = toNormalizedMap(carApiData);
        var normalizedFipeData = toNormalizedMap(fipeData);

        var overrides = enrichmentOverrideRepository
            .findByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(
                request.brand(),
                request.model(),
                request.version()
            );

        var overrideMap = new LinkedHashMap<String, String>();
        overrides.forEach(override -> overrideMap.put(normalizeKey(override.getAttributeName()), override.getAttributeValue()));

        var mergedSources = new LinkedHashMap<String, String>();
        var resolvedAttributes = new LinkedHashMap<String, String>();

        for (var requested : requestedAttributes) {
            var normalized = normalizeKey(requested);

            var mappedFipeKey = mapToFipeCanonical(normalized);
            if (normalizedFipeData.containsKey(mappedFipeKey)) {
                resolvedAttributes.put(requested, normalizedFipeData.get(mappedFipeKey));
                mergedSources.put(requested, "FIPE");
                continue;
            }

            var mappedCarApiKey = mapToCarApiCanonical(normalized);
            if (normalizedCarApiData.containsKey(mappedCarApiKey)) {
                resolvedAttributes.put(requested, normalizedCarApiData.get(mappedCarApiKey));
                mergedSources.put(requested, "CAR_API");
                continue;
            }

            if (overrideMap.containsKey(normalized)) {
                resolvedAttributes.put(requested, overrideMap.get(normalized));
                mergedSources.put(requested, "INTERNAL_DATA_SOURCE");
                continue;
            }

            resolvedAttributes.put(requested, NOT_AVAILABLE);
            mergedSources.put(requested, "NOT_FOUND");
        }

        persistQuery(request, resolvedAttributes, mergedSources);

        return new VehicleQueryResponse(
            request.brand(),
            request.model(),
            request.version(),
            resolvedAttributes,
            mergedSources,
            OffsetDateTime.now()
        );
    }

    private Map<String, String> safeCarApiLookup(VehicleQueryRequest request) {
        try {
            return carApiClient.searchTechnicalAttributes(
                request.brand(),
                request.model(),
                request.version(),
                null
            );
        } catch (ExternalIntegrationException ex) {
            var causeType = ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "n/a";
            var causeMessage = ex.getCause() != null ? ex.getCause().getMessage() : "n/a";
            LOGGER.warn(
                "CarAPI indisponivel para {} {} {} (status={}, causeType={}, causeMessage={}): {}",
                request.brand(),
                request.model(),
                request.version(),
                ex.upstreamStatus(),
                causeType,
                causeMessage,
                ex.getMessage()
            );
            return Map.of();
        }
    }

    private Map<String, String> safeFipeLookup(VehicleQueryRequest request) {
        try {
            var vehicleType = request.vehicleType() == null || request.vehicleType().isBlank()
                ? DEFAULT_VEHICLE_TYPE
                : request.vehicleType();

            var brands = fipeClient.getBrands(vehicleType, request.reference());
            var brand = selectBestBrand(brands, request.brand()).orElse(null);
            if (brand == null) {
                return Map.of();
            }

            var brandId = Integer.parseInt(brand.code());
            var models = fipeClient.getModels(vehicleType, brandId, request.reference());
            var model = selectBestModel(models, request.model(), request.version()).orElse(null);
            if (model == null) {
                return Map.of();
            }

            var modelId = Integer.parseInt(model.code());
            var years = fipeClient.getYears(vehicleType, brandId, modelId, request.reference());
            if (years == null || years.isEmpty()) {
                return Map.of();
            }

            var yearId = years.getFirst().code();
            var priceResponse = fipeClient.getPrice(vehicleType, brandId, modelId, yearId, request.reference());
            if (priceResponse == null) {
                return Map.of();
            }

            var data = new LinkedHashMap<String, String>();
            putIfPresent(data, "Preco", priceResponse.price());
            putIfPresent(data, "Preco de referencia", priceResponse.price());
            putIfPresent(data, "Codigo FIPE", priceResponse.codeFipe());
            putIfPresent(data, "Combustivel", priceResponse.fuel());
            putIfPresent(data, "Referencia FIPE", priceResponse.referenceMonth());
            return data;
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "FIPE indisponivel para {} {} {} (reference={}): {}",
                request.brand(),
                request.model(),
                request.version(),
                request.reference(),
                ex.getMessage()
            );
            return Map.of();
        }
    }

    private Optional<FipeBrandResponse> selectBestBrand(List<FipeBrandResponse> brands, String requestedBrand) {
        if (brands == null || brands.isEmpty()) {
            return Optional.empty();
        }
        var normalizedBrand = normalizeKey(requestedBrand);

        return brands.stream()
            .max(Comparator.comparingInt(brand -> score(normalizeKey(brand.name()), normalizedBrand)));
    }

    private Optional<FipeModelResponse> selectBestModel(List<FipeModelResponse> models, String requestedModel, String requestedVersion) {
        if (models == null || models.isEmpty()) {
            return Optional.empty();
        }
        var normalizedModel = normalizeKey(requestedModel);
        var normalizedVersion = normalizeKey(requestedVersion);

        return models.stream()
            .max(Comparator.comparingInt(model -> {
                var candidate = normalizeKey(model.name());
                return score(candidate, normalizedModel) * 3 + score(candidate, normalizedVersion);
            }));
    }

    private int score(String candidate, String requested) {
        if (requested == null || requested.isBlank() || candidate == null || candidate.isBlank()) {
            return 0;
        }
        if (candidate.equals(requested)) {
            return 3;
        }
        if (candidate.contains(requested)) {
            return 2;
        }
        if (requested.contains(candidate)) {
            return 1;
        }
        return 0;
    }

    private Map<String, String> toNormalizedMap(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        return source.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> normalizeKey(entry.getKey()),
                Map.Entry::getValue,
                (current, replacement) -> current,
                LinkedHashMap::new
            ));
    }

    private Set<String> normalizeRequestedAttributes(Iterable<String> attributes) {
        var ordered = new LinkedHashSet<String>();
        attributes.forEach(attribute -> {
            if (attribute != null && !attribute.isBlank()) {
                ordered.add(attribute.trim());
            }
        });
        return ordered;
    }

    private String mapToCarApiCanonical(String requested) {
        return switch (requested) {
            case "motor" -> "motor";
            case "potencia" -> "potencia";
            case "torque" -> "torque";
            case "transmissao" -> "transmissao";
            case "combustivel" -> "combustivel";
            case "tracao" -> "tracao";
            default -> requested;
        };
    }

    private String mapToFipeCanonical(String requested) {
        return switch (requested) {
            case "preco", "precoreferencia", "precodereferencia", "valor" -> "preco";
            case "codigofipe" -> "codigofipe";
            case "combustivel" -> "combustivel";
            case "referenciafipe", "mesreferencia" -> "referenciafipe";
            default -> requested;
        };
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }

        var normalized = Normalizer.normalize(key, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private void persistQuery(
        VehicleQueryRequest request,
        Map<String, String> attributes,
        Map<String, String> sources
    ) {
        var history = new VehicleQueryHistory();
        history.setBrand(request.brand());
        history.setModel(request.model());
        history.setVersion(request.version());
        history.setRequestedAt(OffsetDateTime.now());

        attributes.forEach((attributeName, attributeValue) -> {
            var item = new VehicleQueryAttributeResult();
            item.setAttributeName(attributeName);
            item.setAttributeValue(attributeValue);
            item.setDataSource(sources.getOrDefault(attributeName, "UNKNOWN"));
            history.addAttribute(item);
        });

        queryHistoryRepository.save(history);
    }
}
