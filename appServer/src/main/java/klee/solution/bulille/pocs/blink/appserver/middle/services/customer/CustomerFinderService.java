package klee.solution.bulille.pocs.blink.appserver.middle.services.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.lang.NonNull;

import java.util.Optional;

@FunctionalInterface
public interface CustomerFinderService {
    Optional<Customer> find(@NonNull CustomerId customerId);
}
