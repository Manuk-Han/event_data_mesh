package module.contract.catalog.dto;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String imageUrl
) {}
