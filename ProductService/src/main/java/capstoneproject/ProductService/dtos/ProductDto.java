package capstoneproject.ProductService.dtos;

import lombok.Getter;
import lombok.Setter;
import capstoneproject.ProductService.models.product.Product;

@Getter
@Setter
public class ProductDto {
    private Long id;
    private String title;
    private String description;
    private double price;
    private int stockQuantity;
    private double rating;
    private String category;

    public static ProductDto fromProduct(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setTitle(product.getTitle());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setStockQuantity(product.getStockQuantity());
        productDto.setRating(product.getRating());
        productDto.setCategory(product.getCategory().getName());
        return productDto;
    }
}
