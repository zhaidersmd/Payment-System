package com.paymentplatform.payment.service;

import com.paymentplatform.payment.entity.Customer;
import com.paymentplatform.payment.entity.Payment;
import com.paymentplatform.payment.exception.PaymentNotFoundException;
import com.paymentplatform.payment.repository.CustomerRepository;
import com.paymentplatform.payment.repository.PaymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("paymentOwnershipService")
public class PaymentOwnershipService {

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;


    public PaymentOwnershipService(CustomerRepository customerRepository, PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
    }

    public boolean canAccessPayment(UUID paymentId, Authentication authentication) {


        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (isAdmin(authentication)) {
            return true;
        }

        Customer customer = customerRepository.findByUserUsername(authentication.getName()).orElse(null);

        if (customer == null) {
            return false;
        }

        return payment.getCustomerId().equals(customer.getCustomerId());
    }

    public boolean canAccessAllPayments(Authentication authentication) {
        return isAdmin(authentication);
    }

    public boolean isAdmin(Authentication authentication) {
        System.out.println("Username: " + authentication.getName());
        System.out.println("Authorities: " + authentication.getAuthorities());
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
    }
}
