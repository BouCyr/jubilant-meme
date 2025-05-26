package klee.solution.bulille.pocs.blink.appserver.middle.process.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.lang.NonNull;

@FunctionalInterface
public interface CustomerCreator {
    Customer createCustomer(@NonNull CustomerInput customerInput);
}
