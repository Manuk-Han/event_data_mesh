package module.common.catalogclient.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "module.common.catalogclient")
public class CatalogClientConfig {}

