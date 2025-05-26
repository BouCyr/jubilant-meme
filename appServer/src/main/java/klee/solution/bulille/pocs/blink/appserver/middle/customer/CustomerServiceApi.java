package klee.solution.bulille.pocs.blink.appserver.middle.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface CustomerServiceApi {

    Optional<Customer> find(@NonNull CustomerId customerId);

    @NonNull
    Page<CustomerOutput> searchCustomers(String nameQuery, @NonNull Pageable pageable);

    @NonNull
    Customer createCustomer(@NonNull CustomerInput customerInput);

    @NonNull
    Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput);
}
