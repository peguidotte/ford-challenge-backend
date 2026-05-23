package br.com.fiap.fordchallengebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.fordchallengebackend.domain.VehicleQueryHistory;

public interface VehicleQueryHistoryRepository extends JpaRepository<VehicleQueryHistory, Long> {
}
