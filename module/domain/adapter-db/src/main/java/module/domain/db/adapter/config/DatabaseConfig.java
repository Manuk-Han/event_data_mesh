package module.domain.db.adapter.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "module.domain.db.repository")
@EntityScan(basePackages = "module.domain.db.entity")
public class DatabaseConfig {
}