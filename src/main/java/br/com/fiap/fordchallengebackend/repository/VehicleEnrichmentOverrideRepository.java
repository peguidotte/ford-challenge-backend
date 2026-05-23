package br.com.fiap.fordchallengebackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.fordchallengebackend.domain.VehicleEnrichmentOverride;

public interface VehicleEnrichmentOverrideRepository extends JpaRepository<VehicleEnrichmentOverride, Long> {

    List<VehicleEnrichmentOverride> findByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(
        String brand,
        String model,
        String version
    );
}
