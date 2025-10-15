package module.contract.catalog.event;

public record ProductRegistered(
        Long productId,
        String name
) {}
