package module.domain.db.adapter.config;

import module.domain.core.port.out.ProductRepositoryPort;
import module.domain.db.adapter.db.ProductRepositoryAdapter;
import module.domain.db.repository.SpringDataProductRepository;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterDbConfig {
    public ProductRepositoryPort productRepositoryPort(SpringDataProductRepository jpaRepo) {
        return new ProductRepositoryAdapter(jpaRepo);
    }
}
