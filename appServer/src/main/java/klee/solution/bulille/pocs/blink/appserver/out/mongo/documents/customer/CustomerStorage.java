package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Component
public class CustomerStorage {

    private final CustomerRepository customerRepository;

    public CustomerStorage(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> find(CustomerId customerId) {

        return this.customerRepository.findById(customerId.toOID());
    }

    public Page<Customer> findByFirstNameOrGivenNameContainingIgnoreCase(String nameQuery, Pageable pageable) {
        return this.customerRepository.findByFirstNameOrGivenNameContainingIgnoreCase(nameQuery, pageable);
    }

    public Page<Customer> findAll(Pageable pageable) {
        return this.customerRepository.findAll(pageable);
    }

    public Customer save(Customer customer) {
        return this.customerRepository.save(customer);
    }
}
