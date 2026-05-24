package br.com.fiap.fordchallengebackend.controller;

import br.com.fiap.fordchallengebackend.dto.VehicleQueryRequest;
import br.com.fiap.fordchallengebackend.dto.VehicleQueryResponse;
import br.com.fiap.fordchallengebackend.service.VehicleSpecificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicle-specs")
@Tag(name = "Vehicle Specifications")
public class VehicleSpecificationController {

    private final VehicleSpecificationService vehicleSpecificationService;

    public VehicleSpecificationController(VehicleSpecificationService vehicleSpecificationService) {
        this.vehicleSpecificationService = vehicleSpecificationService;
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('USER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "Consolida atributos tecnicos por marca, modelo, versao e lista dinamica de atributos")
    public ResponseEntity<VehicleQueryResponse> query(@Valid @RequestBody VehicleQueryRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(vehicleSpecificationService.query(request));
    }
}
