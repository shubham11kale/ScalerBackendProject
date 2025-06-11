package capstoneproject.ProductService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import capstoneproject.ProductService.models.cart.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
