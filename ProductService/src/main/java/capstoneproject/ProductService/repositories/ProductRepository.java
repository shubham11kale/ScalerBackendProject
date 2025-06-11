package capstoneproject.ProductService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import capstoneproject.ProductService.models.product.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
