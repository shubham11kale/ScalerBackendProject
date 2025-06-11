package capstoneproject.ProductService.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import capstoneproject.ProductService.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
