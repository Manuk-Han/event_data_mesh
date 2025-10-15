package module.common.catalogclient.service;

import lombok.RequiredArgsConstructor;
import module.common.catalogclient.common.CatalogFeign;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final CatalogFeign catalog;
    public String loadCatalogSchema() {
        return catalog.getSchema("catalog-product-v1").getContent();
    }
}

