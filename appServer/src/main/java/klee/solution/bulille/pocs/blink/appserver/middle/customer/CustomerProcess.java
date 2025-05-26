package klee.solution.bulille.pocs.blink.appserver.middle.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
// import klee.solution.bulille.pocs.blink.appserver.middle.CustomerService; // Will be replaced by CustomerServiceApi
import klee.solution.bulille.pocs.blink.appserver.middle.customer.CustomerServiceApi; // New import
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerProcess.class);
    private final CustomerServiceApi customerService; // Changed type to CustomerServiceApi

    public CustomerProcess(CustomerServiceApi customerService) { // Changed type in constructor
        this.customerService = customerService;
    }

    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("processFind called with customerId: {}", customerId);
        Optional<Customer> result = this.customerService.find(customerId); // No change in call needed if field name is the same
        LOGGER.info("processFind finished for customerId: {}", customerId);
        return result;
    }

    // nameQuery can be null or empty, so it's not annotated with @NonNull
    public @NonNull Page<CustomerOutput> searchCustomers(String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("processSearchCustomers called with nameQuery: {}, pageable: {}", nameQuery, pageable);
        Page<CustomerOutput> results = this.customerService.searchCustomers(nameQuery, pageable); // No change in call
        LOGGER.info("processSearchCustomers finished, found {} customers", results.getTotalElements());
        return results;
    }

    public @NonNull Customer createCustomer(@NonNull CustomerInput customerInput) {
        // Assuming CustomerInput has firstName and givenName methods for logging,
        // or using toString of CustomerInput if available and appropriate.
        // For this example, I'll log a generic message or part of the input if safe.
        // If CustomerInput is a record, customerInput.firstName() and customerInput.givenName() would work.
        // The example shows customerInput.givenName() - I'll adapt based on typical record/DTO access.
        // Given CustomerInput was refactored to a record, customerInput.givenName() is correct.
        LOGGER.info("processCreateCustomer called for givenName: {}", customerInput.givenName());
        Customer customer = this.customerService.createCustomer(customerInput); // No change in call
        // Assuming customer.id() is the correct way to get the ID, which is typical for entities/records.
        LOGGER.info("processCreateCustomer finished, customerId: {}", customer.id());
        return customer;
    }

    public @NonNull Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput) {
        LOGGER.info("processAddContract called for customerId: {}", customerId);
        Customer customer = this.customerService.addContract(customerId, contractInput); // No change in call
        LOGGER.info("processAddContract finished for customerId: {}", customerId);
        return customer;
    }
}
