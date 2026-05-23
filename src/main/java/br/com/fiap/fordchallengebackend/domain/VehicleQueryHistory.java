package br.com.fiap.fordchallengebackend.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_query_history")
public class VehicleQueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String version;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @OneToMany(mappedBy = "queryHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<VehicleQueryAttributeResult> attributes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public List<VehicleQueryAttributeResult> getAttributes() {
        return attributes;
    }

    public void addAttribute(VehicleQueryAttributeResult attribute) {
        attribute.setQueryHistory(this);
        this.attributes.add(attribute);
    }
}
