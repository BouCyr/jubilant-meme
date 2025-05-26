package klee.solution.bulille.pocs.blink.appserver.middle.services.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import java.util.Optional;

public interface CustomerServicing {
    Optional<Customer> find(@NonNull CustomerId customerId);
    Page<CustomerOutput> searchCustomers(@NonNull String nameQuery, @NonNull Pageable pageable);
    Customer createCustomer(@NonNull CustomerInput customerInput);
    Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput);
}
