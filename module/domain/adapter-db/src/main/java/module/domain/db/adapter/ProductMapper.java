package module.domain.db.adapter;

import module.domain.core.domain.Product;
import module.domain.db.entity.ProductJpaEntity;

public class ProductMapper {
    public static ProductJpaEntity toJpaEntity(Product product) {
        return ProductJpaEntity.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .fileUrl(product.getFileUrl())
                .fileKey(product.getFileKey())
                .build();
    }

    public static Product toDomain(ProductJpaEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .fileUrl(entity.getFileUrl())
                .fileKey(entity.getFileKey())
                .build();
    }
}
