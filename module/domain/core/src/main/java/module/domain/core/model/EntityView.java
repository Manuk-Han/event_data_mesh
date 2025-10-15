package module.domain.core.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class EntityView {
    Long id;

    String name;

    String label;

    @Singular("attr")
    Map<String, Object> attrs;
}
