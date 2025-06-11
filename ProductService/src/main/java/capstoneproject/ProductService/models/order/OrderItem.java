package capstoneproject.ProductService.models.order;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import capstoneproject.ProductService.models.BaseModel;
import capstoneproject.ProductService.models.product.Product;

@Getter
@Setter
@Entity
public class OrderItem extends BaseModel {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    private int quantity;
    private double priceAtPurchase;
}
