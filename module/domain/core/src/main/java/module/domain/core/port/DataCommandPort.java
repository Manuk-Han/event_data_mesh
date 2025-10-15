package module.domain.core.port;

import module.domain.core.model.EntityView;
import java.util.Map;

public interface DataCommandPort {
    EntityView create(String name, String label, Map<String,Object> attrs);
}
