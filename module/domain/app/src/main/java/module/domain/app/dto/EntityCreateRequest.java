package module.domain.app.dto;

import java.util.Map;

public record EntityCreateRequest(String name, String label, Map<String,Object> attrs) { }
