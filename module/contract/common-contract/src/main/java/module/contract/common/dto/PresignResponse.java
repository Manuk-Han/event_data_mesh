package module.contract.common.dto;

public record PresignResponse(
        String url,
        long expiresInSeconds
) {}
