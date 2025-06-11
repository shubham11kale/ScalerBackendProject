package capstoneproject.ProductService.services;

import org.springframework.stereotype.Service;
import capstoneproject.ProductService.dtos.AddCartItemtDto;
import capstoneproject.ProductService.exceptions.InsufficientStockException;
import capstoneproject.ProductService.exceptions.InvalidDataException;
import capstoneproject.ProductService.exceptions.NotFoundException;
import capstoneproject.ProductService.models.User;
import capstoneproject.ProductService.models.cart.Cart;
import capstoneproject.ProductService.models.cart.CartItem;
import capstoneproject.ProductService.models.product.Product;
import capstoneproject.ProductService.repositories.CartItemRepository;
import capstoneproject.ProductService.repositories.CartRepository;
import capstoneproject.ProductService.repositories.ProductRepository;
import capstoneproject.ProductService.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public Cart addItemsToCart(Long userId, AddCartItemtDto addCartItemtDto) throws NotFoundException,
            InsufficientStockException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for id:"+userId));

        Product product = productRepository.findById(addCartItemtDto.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found for id:"
                        +addCartItemtDto.getProductId()));
        if(product.isDeleted())
        {throw new NotFoundException("Product not found for id:"+product.getId());}

        //Validate Stock
        if (product.getStockQuantity() < addCartItemtDto.getQuantity()) {
            throw new InsufficientStockException("Product stock is insufficient");
        }

        // Get or create the user's cart
        Cart cart = user.getCart();
        if(cart == null)
        {
            cart = new Cart();
            cart.setUser(user);
            user.setCart(cart); // Bidirectional sync
        }

        // Check if item already exists in the cart
        Optional<CartItem> existingItem = cart.getProducts().stream().
                filter(item -> item.getProduct().getId().equals(addCartItemtDto.getProductId())).findFirst();

        if(existingItem.isPresent()) {
            // Update existing item's quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + addCartItemtDto.getQuantity());
        }
        else {
            CartItem item = new CartItem();
            item.setQuantity(addCartItemtDto.getQuantity());
            item.setProduct(product);
            item.setCart(cart);
            cart.getProducts().add(item);
        }

        cart.setTotalPrice(calculateCartTotal(cart));
        return cartRepository.save(cart);
    }

    public Cart getCartItems(Long userId) throws NotFoundException, InvalidDataException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for id:"+userId));
        // Get or create the user's cart
        Cart cart = user.getCart();
        if(cart == null)
        {
            throw new InvalidDataException("Cart is empty for user: "+ userId);
        }
        return cart;
    }

    public Cart removeItemsFromCart(Long userId, Long cartItemId) throws NotFoundException, InvalidDataException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for id:"+userId));

        Cart cart = user.getCart();
        if(cart == null || cart.getProducts().isEmpty())
        {
            throw new NotFoundException("Cart is empty for user: "+ userId);
        }

        CartItem itemToRemove = cart.getProducts().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found for id: "+cartItemId));

        // Remove the item from the list (orphan removal handles deletion)
        cart.getProducts().remove(itemToRemove);
        //commenting as orphan removal and cascading will be done automatically as it is captured in annotations for cartItems in cart Entity
        //cartItemRepository.delete(itemToRemove); // Orphan removal handles this automatically
        cart.setTotalPrice(calculateCartTotal(cart));
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantityInCart(Long userId, Long cartItemId, int updateQuantity)
            throws NotFoundException, InvalidDataException, InsufficientStockException {
        if(updateQuantity<1)
        {
            throw new InvalidDataException("Invalid quantity.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for id:"+userId));

        Cart cart = user.getCart();
        if(cart == null || cart.getProducts().isEmpty())
        {
            throw new NotFoundException("Cart is empty for user: "+ userId);
        }

        CartItem itemToUpdate = cart.getProducts().stream()
                .filter(item -> item.getProduct().getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found for item id: "+ cartItemId));

        Product product = itemToUpdate.getProduct();
        if(product.getStockQuantity() < updateQuantity) {
            throw new InsufficientStockException("Insufficient stock for product: "+product.getId());
        }

        itemToUpdate.setQuantity(updateQuantity);
        cart.setTotalPrice(calculateCartTotal(cart));
        return cartRepository.save(cart);
    }

    public void clearCart(Long userId) throws NotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for id:"+userId));

        Cart cart = user.getCart();
        if(cart!=null)
        {
            //cascading and orphan removal will be done automatically
            cart.getProducts().clear();
            cart.setTotalPrice(0.0);
            cartRepository.save(cart);
        }
    }

    private double calculateCartTotal(Cart cart) {
        List<CartItem> cartItems = cart.getProducts();
        double total = 0;
        for (CartItem cartItem : cartItems) {
            total += cartItem.getProduct().getPrice() * cartItem.getQuantity();
        }
        return total;
    }

}
