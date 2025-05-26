package klee.solution.bulille.pocs.blink.appserver.middle.services.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.lang.NonNull;

@FunctionalInterface
public interface ContractAdderService {
    Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput);
}
