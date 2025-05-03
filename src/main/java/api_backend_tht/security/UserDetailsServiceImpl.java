package api_backend_tht.security;

import api_backend_tht.repository.UserRepository;
import api_backend_tht.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> userRoleRepository.findRolesByUserId(user.getId())
                        .collectList()
                        .map(roles -> {
                            user.setRoles(roles);
                            return user;
                        })
                )
                .map(UserDetailsImpl::build);
    }
}