package capstoneproject.UserService.repositories;

import capstoneproject.UserService.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenValueAndExpiryDateGreaterThan(String tokenValue, Date expiryAt);
}
