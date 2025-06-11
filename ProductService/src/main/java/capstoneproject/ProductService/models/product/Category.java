package capstoneproject.ProductService.models.product;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import capstoneproject.ProductService.models.BaseModel;

@Getter
@Setter
@Entity
public class Category extends BaseModel {
    private String name;
}
