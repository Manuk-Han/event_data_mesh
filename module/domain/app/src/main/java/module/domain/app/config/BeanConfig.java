package module.domain.app.config;

import module.domain.core.port.DataQueryPort;
import module.domain.core.port.SearchEntitiesUseCase;
import module.domain.core.service.SearchEntitiesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public SearchEntitiesUseCase searchEntitiesUseCase(DataQueryPort port) {
        return new SearchEntitiesService(port);
    }
}