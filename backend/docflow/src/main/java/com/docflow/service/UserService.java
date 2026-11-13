
package com.docflow.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.docflow.entity.Role;
import com.docflow.entity.User;
import com.docflow.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String name, String email, String password) {

        if (userRepository.existsByEmail(email)) {
        	throw new ResponseStatusException(
        		    HttpStatus.CONFLICT,
        		    "Email already registered"
        		);
        }

        User user = new User();

        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

	public User authenticateUser(String email, String password) {
	
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("Invalid email or password"));
	
	    if (!passwordEncoder.matches(password, user.getPassword())) {
	        throw new RuntimeException("Invalid email or password");
	    }
	
	    return user;
	}

}
