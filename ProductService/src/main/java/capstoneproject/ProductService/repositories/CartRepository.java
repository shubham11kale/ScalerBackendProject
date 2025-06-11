package capstoneproject.ProductService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import capstoneproject.ProductService.models.cart.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
}
