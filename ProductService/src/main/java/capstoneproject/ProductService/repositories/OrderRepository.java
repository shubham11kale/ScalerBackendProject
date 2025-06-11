package capstoneproject.ProductService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import capstoneproject.ProductService.models.order.Order;
import capstoneproject.ProductService.models.order.OrderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByPaymentOrderId(String paymentOrderId);

    Optional<List<Order>> findByUserId(Long user_id);

    Optional<List<Order>> findByUserIdAndOrderStatus(Long user_id, OrderStatus orderStatus);
}
