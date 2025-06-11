package capstoneproject.ProductService.controllers;

import capstoneproject.ProductService.dtos.CreateProductDto;
import capstoneproject.ProductService.dtos.ProductDto;
import capstoneproject.ProductService.exceptions.InvalidDataException;
import capstoneproject.ProductService.exceptions.NotFoundException;
import capstoneproject.ProductService.exceptions.ResourceAccessForbidden;
import capstoneproject.ProductService.models.User;
import capstoneproject.ProductService.models.product.Product;
import capstoneproject.ProductService.repositories.UserRepository;
import capstoneproject.ProductService.services.ProductService;
import capstoneproject.ProductService.utils.UserUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/products")
public class ProductController {

    private ProductService productService;
    private UserRepository userRepository;
    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @PostMapping("/product")
    public ResponseEntity<ProductDto> crateProduct(Authentication authentication,
                                                   @RequestBody CreateProductDto createProductDto)
            throws InvalidDataException, ResourceAccessForbidden {

        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        User user = UserUtils.createUserIfNotExist(jwt, userRepository);

        if(!(user.getRoles().contains("ADMIN")) && (!user.getRoles().contains("SUPER_ADMIN")) ) {
            throw new ResourceAccessForbidden("Not Allowed to create product");
        }
        Product product = productService.createProduct(createProductDto);
        return new ResponseEntity<>(ProductDto.fromProduct(product), HttpStatus.CREATED);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<ProductDto> getProduct(Authentication authentication, @PathVariable long id) throws NotFoundException {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        UserUtils.createUserIfNotExist(jwt, userRepository);

        Product product = productService.getProductById(id);
        return new ResponseEntity<>(ProductDto.fromProduct(product), HttpStatus.OK);
    }

    @GetMapping("/product/all")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ProductDto> productDtoList = new ArrayList<>();
        for (Product product : products) {
            productDtoList.add(ProductDto.fromProduct(product));
        }
        return new ResponseEntity<>(productDtoList, HttpStatus.OK);
    }

    @PatchMapping("/product/{id}")
    public ResponseEntity<ProductDto> updateProduct(Authentication authentication, @PathVariable long id,
                                                    @RequestBody Map<String, Object> updates) throws NotFoundException, ResourceAccessForbidden {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        User user = UserUtils.createUserIfNotExist(jwt, userRepository);

        if(!(user.getRoles().contains("ADMIN")) && (!user.getRoles().contains("SUPER_ADMIN")) ) {
            throw new ResourceAccessForbidden("Not Allowed to Update product");
        }
        Product product = productService.updateProduct(id, updates);
        return new ResponseEntity<>(ProductDto.fromProduct(product), HttpStatus.OK);
    }
}
