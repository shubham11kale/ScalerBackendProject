package capstoneproject.ProductService.utils;

import org.springframework.security.oauth2.jwt.Jwt;
import capstoneproject.ProductService.models.User;
import capstoneproject.ProductService.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

public class UserUtils {

    public  static User createUserIfNotExist(Jwt jwtToken, UserRepository userRepository) {
        String userId = jwtToken.getClaim("userId");
        Optional<User> optionalUser = userRepository.findById(Long.valueOf(userId));
        if(optionalUser.isPresent()) {
            return optionalUser.get();
        }
        String email = jwtToken.getClaim("sub");
        String address = jwtToken.getClaim("address");
        String name = jwtToken.getClaim("name");
        List<String> role = jwtToken.getClaim("roles");


        User user = new User();
        user.setEmail(email);
        user.setId(Long.valueOf(userId));
        user.setAddress(address);
        user.setName(name);
        user.setRoles(role);
        return userRepository.save(user);
    }
}
