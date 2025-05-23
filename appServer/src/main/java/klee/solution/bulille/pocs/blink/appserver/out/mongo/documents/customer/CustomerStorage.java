package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import org.springframework.stereotype.Component;

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
}
