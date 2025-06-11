package capstoneproject.ProductService.services;

import capstoneproject.ProductService.dtos.AddCartItemtDto;
import capstoneproject.ProductService.dtos.PaymentClientDto;
import capstoneproject.ProductService.exceptions.InsufficientStockException;
import capstoneproject.ProductService.exceptions.NotFoundException;
import capstoneproject.ProductService.exceptions.PaymentClientException;
import capstoneproject.ProductService.models.User;
import capstoneproject.ProductService.models.cart.CartItem;
import capstoneproject.ProductService.models.order.*;
import capstoneproject.ProductService.models.product.Product;
import capstoneproject.ProductService.paymentservice.PaymentServiceClient;
import capstoneproject.ProductService.producer.EventProducer;
import capstoneproject.ProductService.producer.OrderCompletedEvent;
import capstoneproject.ProductService.producer.RefundEvent;
import capstoneproject.ProductService.repositories.OrderRepository;
import capstoneproject.ProductService.repositories.ProductRepository;
import capstoneproject.ProductService.repositories.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CartService cartService;
    private PaymentServiceClient paymentServiceClient;
    private final EventProducer eventProducer;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        UserRepository userRepository, CartService cartService,
                        @Qualifier("razorpayPaymentClient") PaymentServiceClient paymentServiceClient,
                        EventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.paymentServiceClient = paymentServiceClient;
        this.eventProducer = eventProducer;
    }


    public Order placeOrder(Long userId) throws NotFoundException, InsufficientStockException, PaymentClientException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for id:"+userId));

        Cart cart = user.getCart();
        if(cart == null || cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new NotFoundException("Cart is Empty");
        }

        // Create the order
        Order order = new Order();
        order.setUser(user);

        // Validate stock
        // Deduct stock
        // Create order item
        for (CartItem item : cart.getProducts()) {
            Product product = item.getProduct();

            // Validate stock
            if (product.getStockQuantity() < item.getQuantity())
            {
                throw new InsufficientStockException("Insufficient stock for product: "+ product.getTitle());
            }


            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);
        }

        // calculate total and create payment intent transaction id
        double totalAmount = order.getOrderItems().stream()
                .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                .sum();
        order.setTotalAmount(totalAmount);

        String invoiceNumber = RandomStringUtils.randomAlphanumeric(15);

        PaymentClientDto paymentClientDto = paymentServiceClient.createPaymentOrder(invoiceNumber,"INR", totalAmount);

        order.setInvoiceNumber(invoiceNumber);
        order.setPaymentOrderId(paymentClientDto.getPaymentOrderId());
        order.setPaymentLink(paymentClientDto.getPaymentLink());
        order.setPaymentMethod(PaymentMethod.ONLINE);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTrackingNumber(RandomStringUtils.randomAlphanumeric(20));
        order.setOrderStatus(OrderStatus.PENDING);

        // Save the order
        orderRepository.save(order);

        // Clear the cart
        cartService.clearCart(userId);

        // Return the order with payment details (client completes payment on the frontend)
        return order;
    }

    public Order confirmPayment(Long userId, String paymentOrderId) throws NotFoundException, PaymentClientException {

        Order order = orderRepository.findByPaymentOrderId(paymentOrderId)
                .orElseThrow(() -> new NotFoundException("Order not found for id:"+paymentOrderId));

        // Check if the order is already cancelled
        if (!OrderStatus.PENDING.equals(order.getOrderStatus())) {
            throw new IllegalStateException("Order is not waiting for payment status");
        }

        // Check if the order is already cancelled
        if (!PaymentStatus.PENDING.equals(order.getPaymentStatus())) {
            throw new IllegalStateException("Order payment status already provided. Payment status: "+order.getPaymentStatus());
        }

        PaymentClientDto paymentClientDto = paymentServiceClient.getPaymentStatus(paymentOrderId);

        //PaymentId will get updated only when payment is tried whether pass or fail
        if( paymentClientDto.getPaymentId() == null || paymentClientDto.getPaymentId().isBlank())
        {
            throw new IllegalStateException("Payment is not attempted.");
        }

        if("paid".equals(paymentClientDto.getStatus()))
        {
            // Send event to Kafka
            OrderCompletedEvent event = new OrderCompletedEvent(order.getId().toString(), order.getUser().getEmail(), "Your order has been completed.");
            eventProducer.sendOrderCompletedEvent(event);
            System.out.println("Kafka Event sent.");
            //Payment success
            order.setPaymentId(paymentClientDto.getPaymentId());
            order.setPaymentLink(null);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.PLACED);
            return orderRepository.save(order);
        }
        else {
            //payment failed
            //Restore product stock quantity and user cart items
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
                productRepository.save(product);

                AddCartItemtDto addCartItemtDto = new AddCartItemtDto();
                addCartItemtDto.setProductId(product.getId());
                addCartItemtDto.setQuantity(orderItem.getQuantity());
                try {
                    cartService.addItemsToCart(userId, addCartItemtDto);
                } catch (InsufficientStockException ex) {
                    //This scenario will occur when after adding stock other customer purchased it before adding it cart
                    System.out.println("Insufficient stock for product: " + orderItem.getProduct().getTitle());
                }
            }
            order.setPaymentId(paymentClientDto.getPaymentId());
            order.setPaymentLink(null);
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.FAILED);
            return orderRepository.save(order);
        }
    }

    public Order getOrder(Long orderId) throws NotFoundException {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found for id:"+orderId));
    }

    public List<Order> getAllOrders(Long userId, OrderStatus status) throws NotFoundException {

        Optional<List<Order>> orders = (status != null)
                ? orderRepository.findByUserIdAndOrderStatus(userId, status) // Filter by status
                : orderRepository.findByUserId(userId); // Fetch all orders

        if(orders.isEmpty()) {
            throw new NotFoundException("Order not found for id:"+userId);
        }
        return orders.get();
    }

    public Order cancelOrder(Long orderId) throws NotFoundException, PaymentClientException  {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found for id:"+orderId));

        // Check if the order is already cancelled
        if (OrderStatus.CANCELLED.equals(order.getOrderStatus())) {
            throw new IllegalStateException("Order is already cancelled");
        }

        // If the order was paid, initiate a refund
        if (PaymentStatus.PAID.equals(order.getPaymentStatus())) {
            //Initiate & Perform Refund
            PaymentClientDto paymentClientDto = paymentServiceClient.processRefund(order.getPaymentOrderId());
            RefundEvent refundEvent = new RefundEvent(order.getId().toString(),order.getUser().getEmail(),"You order has been cancelled", String.valueOf(order.getTotalAmount()));
            eventProducer.sendRefundEvent(refundEvent);
            order.setRefundId(paymentClientDto.getRefundId());
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        //When order is cancelled before confirming payment
        else {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
            order.setPaymentLink(null);
        }

        // Restore stock for each item in the order
        order.getOrderItems().forEach(orderItem -> {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productRepository.save(product);
        });

        order.setOrderStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

}
