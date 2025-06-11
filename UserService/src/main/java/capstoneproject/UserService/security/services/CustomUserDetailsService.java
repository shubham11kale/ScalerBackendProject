package capstoneproject.UserService.security.services;

import capstoneproject.UserService.models.User;
import capstoneproject.UserService.repositories.UserRepository;
import capstoneproject.UserService.security.models.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private UserRepository userRepository;
    @Autowired
    public CustomUserDetailsService (UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> optionalUser = userRepository.findByEmail(username);

        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException(
                    "user_name "+username+" does not exists!"
            );
        }

        return new CustomUserDetails(optionalUser.get());
    }
}
