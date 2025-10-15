package module.domain.app.usecase;

import lombok.RequiredArgsConstructor;
import module.contract.catalog.dto.ProductRequest;
import module.contract.catalog.dto.ProductResponse;
import module.contract.catalog.dto.SchemaDto;
import module.contract.common.dto.PageResponse;
import module.domain.app.feign.CatalogFeign;
import module.domain.app.validator.JsonPayloadValidator;
import module.domain.core.port.out.ObjectStoragePort;
import module.domain.core.port.out.ProductRepositoryPort;
import module.domain.core.domain.Product;
import module.domain.storage.s3.S3Props;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductCommandService {
    @Qualifier("productRepositoryAdapter")
    private final ProductRepositoryPort repo;

    private final ObjectStoragePort storage;

    private final S3Props props;

    private final CatalogFeign catalogFeign;
    private final JsonPayloadValidator validator;

    @Transactional(readOnly = true)
    public Map<String, String> issueDownloadUrl(Long productId, Duration ttl) {
        // 1) 인증/권한/소유 검증 (예: SecurityContext, team, role 등)
        // if (!hasPermission(user, productId)) throw new AccessDeniedException(...);

        Product p = repo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product not found: " + productId));
        if (p.getFileUrl() == null) throw new IllegalStateException("no image for product");

        String key = (p.getFileKey() != null)
                ? p.getFileKey()
                : extractKeyFromUrl(p.getFileUrl(), props.getPublicBaseUrl());

        String url = storage.presignGet(key, ttl);
        return Map.of("url", url, "expiresInSeconds", String.valueOf(ttl.toSeconds()));
    }

    private String extractKeyFromUrl(String url, String base) {
        String prefix = base.endsWith("/") ? base : base + "/";
        if (!url.startsWith(prefix)) throw new IllegalArgumentException("unexpected imageUrl");
        return url.substring(prefix.length());
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        SchemaDto schema = catalogFeign.getSchema("catalog-product-v1");

        if ("JSONSCHEMA".equalsIgnoreCase(schema.getFormat())) {
            validator.validate(schema.getContent(), req);
        }

        Product product = Product.builder()
                .name(req.name())
                .description(req.description())
                .build();
        Product saved = repo.save(product);

        return new ProductResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getFileUrl());
    }

    @Transactional
    public ProductResponse attachProduct(Long productId, String filename, String contentType, InputStream in, long size) {
        Product product = repo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product not found: " + productId));

        String key = "product/%d/%s".formatted(productId, filename);
        String url = storage.put(key, in, size, contentType);

        product.setFileKey(key);
        product.setFileUrl(url);
        Product saved = repo.save(product);

        return new ProductResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.getFileUrl());
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("product not found: " + id));
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getFileUrl());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getPage(int page, int size) {
        var items = repo.findAll(page, size).stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getFileUrl()))
                .toList();
        long total = repo.countAll();
        return PageResponse.<ProductResponse>builder()
                .page(page)
                .size(size)
                .total(total)
                .items(items)
                .build();
    }
}
