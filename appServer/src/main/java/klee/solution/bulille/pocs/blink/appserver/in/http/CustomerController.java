package klee.solution.bulille.pocs.blink.appserver.in.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.customer.CustomerProcess; // Import CustomerProcess
import klee.solution.bulille.pocs.blink.appserver.middle.customer.impl.CustomerServiceImpl; // For CustomerNotFoundException (remains for now)
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
// import klee.solution.bulille.pocs.blink.appserver.middle.customer.CustomerServiceApi; // No longer directly used
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
// CustomerId is already imported


import java.util.Optional;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CustomerController.class);

    private final CustomerProcess customerProcess; // Changed to CustomerProcess

    public CustomerController(CustomerProcess customerProcess) { // Changed constructor to use CustomerProcess
        this.customerProcess = customerProcess;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOutput> getCustomer(@PathVariable("id") String id){
        LOGGER.info("getCustomer called with id: {}", id);
        var customerId = new CustomerId(id);
        Optional<Customer> customer = this.customerProcess.find(customerId); // Call customerProcess

        return customer
                .map(CustomerOutput::from)
                .map(customerOutput -> {
                    LOGGER.info("getCustomer successfully returned customer data for id: {}", id);
                    return ResponseEntity.ok(customerOutput);
                })
                .orElseGet(() -> {
                    LOGGER.warn("Customer not found for id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerOutput>> searchCustomers(
            @RequestParam(name = "name", required = false) String nameQuery,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        LOGGER.info("searchCustomers called with nameQuery: {}, page: {}, size: {}", nameQuery, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerOutput> customers = this.customerProcess.searchCustomers(nameQuery, pageable); // Call customerProcess
        LOGGER.info("searchCustomers successfully returned {} customers", customers.getTotalElements());
        return ResponseEntity.ok(customers);
    }

    @PostMapping
    public ResponseEntity<CustomerOutput> createCustomer(@RequestBody CustomerInput customerInput) {
        LOGGER.info("createCustomer called with customerInput: {}", customerInput);
        // Basic validation example (can be enhanced with @Valid and validation annotations on DTO)
        if (customerInput == null || customerInput.firstName() == null || customerInput.firstName().trim().isEmpty() || // Assuming CustomerInput is a record
            customerInput.givenName() == null || customerInput.givenName().trim().isEmpty()) { // Assuming CustomerInput is a record
            LOGGER.warn("createCustomer called with invalid input: {}", customerInput);
            return ResponseEntity.badRequest().build(); // Or throw a custom exception
        }
        try {
            Customer createdCustomer = this.customerProcess.createCustomer(customerInput); // Call customerProcess
            LOGGER.info("createCustomer successfully created customer with id: {}", createdCustomer.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(CustomerOutput.from(createdCustomer));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error creating customer: {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // Or a more specific error response
        } catch (Exception e) {
            LOGGER.error("Unexpected error creating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{customerId}/contracts")
    public ResponseEntity<?> addContractToCustomer(
            @PathVariable String customerId,
            @RequestBody ContractInput contractInput) {
        LOGGER.info("addContractToCustomer called for customerId: {} with contractInput: {}", customerId, contractInput);
        try {
            Customer updatedCustomer = this.customerProcess.addContract(new CustomerId(customerId), contractInput); // Call customerProcess
            LOGGER.info("addContractToCustomer successfully added contract to customer: {}", customerId);
            return ResponseEntity.ok(CustomerOutput.from(updatedCustomer));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error adding contract to customer {}: {}", customerId, e.getMessage());
            // Consider different HTTP status for different errors (e.g., 404 for customer not found vs 400 for bad input)
            return ResponseEntity.badRequest().body(e.getMessage()); // Or a proper error DTO with e.getMessage()
        } catch (CustomerServiceImpl.CustomerNotFoundException e) { // Updated exception type
            LOGGER.warn("Customer not found for id: {} when trying to add contract", customerId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) { // Catch unexpected errors
            LOGGER.error("Unexpected error adding contract to customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
