package klee.solution.bulille.pocs.blink.appserver.middle;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerStorage;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerService {

    private final CustomerStorage customerStorage;

    public CustomerService(CustomerStorage customerStorage) {
        this.customerStorage = customerStorage;
    }

    public Optional<Customer> find(CustomerId customerId) {
        return this.customerStorage.find(customerId);
    }
}
