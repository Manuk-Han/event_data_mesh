package module.domain.db.adapter.db;

import lombok.RequiredArgsConstructor;
import module.domain.core.domain.Product;
import module.domain.db.entity.ProductJpaEntity;
import module.domain.db.adapter.ProductMapper;
import module.domain.core.port.out.ProductRepositoryPort;
import module.domain.db.repository.SpringDataProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {
    private final SpringDataProductRepository jpaRepo;

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = ProductMapper.toJpaEntity(product);
        ProductJpaEntity saved = jpaRepo.save(entity);

        return ProductMapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepo.findById(id).map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findAll(int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(0, page), Math.max(1, size));
        var slice = jpaRepo.findAll(pageable);
        return slice.getContent().stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return jpaRepo.count();
    }
}
