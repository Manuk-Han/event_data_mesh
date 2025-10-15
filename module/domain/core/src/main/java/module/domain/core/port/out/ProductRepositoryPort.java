package module.domain.core.port.out;

import module.domain.core.domain.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAll(int page, int size);
    long countAll();
}
