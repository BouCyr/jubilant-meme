package klee.solution.bulille.pocs.blink.appserver.middle.process.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.lang.NonNull;

import java.util.Optional;

@FunctionalInterface
public interface CustomerFinder {
    Optional<Customer> find(@NonNull CustomerId customerId);
}
