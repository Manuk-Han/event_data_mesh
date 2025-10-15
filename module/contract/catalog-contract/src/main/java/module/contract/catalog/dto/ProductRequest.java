package module.contract.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductRequest(
        @NotBlank
        String name,
        String description
) {
}
