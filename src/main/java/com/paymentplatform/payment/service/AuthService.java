package com.paymentplatform.payment.service;

import com.paymentplatform.payment.dto.AuthResponse;
import com.paymentplatform.payment.dto.LoginRequest;
import com.paymentplatform.payment.dto.RegisterRequest;
import com.paymentplatform.payment.entity.Customer;
import com.paymentplatform.payment.entity.User;
import com.paymentplatform.payment.repository.CustomerRepository;
import com.paymentplatform.payment.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;


    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtService, PasswordEncoder passwordEncoder, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        User saved = userRepository.save(user);
        Customer customer = new Customer();
        customer.setUser(saved);
        customer.setCustomerId(saved.getUsername());
        customerRepository.save(customer);

        return new AuthResponse("User registered successfully");

    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token);
    }
}
