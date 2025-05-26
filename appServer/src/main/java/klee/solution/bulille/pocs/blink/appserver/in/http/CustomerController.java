package klee.solution.bulille.pocs.blink.appserver.in.http;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.process.CustomerProcessing; // Modified import
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final CustomerProcessing customerProcess; // Changed type to CustomerProcessing

    public CustomerController(CustomerProcessing customerProcess) { // Changed constructor parameter type
        this.customerProcess = customerProcess;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOutput> getCustomer(@PathVariable("id") String id){
        LOGGER.info("getCustomer called with id: {}", id);
        try {
            var customerId = new CustomerId(id);
            Optional<Customer> customer = this.customerProcess.find(customerId);

            if (customer.isPresent()) {
                LOGGER.info("getCustomer found customer for id: {}", id);
                return ResponseEntity.ok(CustomerOutput.from(customer.get()));
            } else {
                LOGGER.info("getCustomer did not find customer for id: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("getCustomer failed due to bad request for id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getCustomer for id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerOutput>> searchCustomers(
            @RequestParam(name = "name", required = false) String nameQuery,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        LOGGER.info("searchCustomers called with nameQuery: {}, page: {}, size: {}", nameQuery, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CustomerOutput> customers = this.customerProcess.searchCustomers(nameQuery, pageable);
            LOGGER.info("searchCustomers returning {} customers.", customers.getTotalElements());
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in searchCustomers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<CustomerOutput> createCustomer(@RequestBody CustomerInput customerInput) {
        LOGGER.info("createCustomer called with firstName: {}", customerInput.firstName());
        try {
            // Basic validation example (can be enhanced with @Valid and validation annotations on DTO)
            if (customerInput == null || customerInput.firstName() == null || customerInput.firstName().trim().isEmpty() ||
                customerInput.givenName() == null || customerInput.givenName().trim().isEmpty()) {
                LOGGER.warn("createCustomer failed due to missing required fields: {}", customerInput);
                return ResponseEntity.badRequest().build(); // Or throw a custom exception
            }
            Customer createdCustomer = this.customerProcess.createCustomer(customerInput);
            LOGGER.info("createCustomer completed successfully for customerId: {}", createdCustomer.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(CustomerOutput.from(createdCustomer));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("createCustomer failed due to bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().build(); // Or a more specific error response
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createCustomer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{customerId}/contracts")
    public ResponseEntity<?> addContractToCustomer(
            @PathVariable String customerId,
            @RequestBody ContractInput contractInput) {
        LOGGER.info("addContractToCustomer called for customerId: {}", customerId);
        try {
            Customer updatedCustomer = this.customerProcess.addContract(new CustomerId(customerId), contractInput);
            LOGGER.info("addContractToCustomer completed successfully for customerId: {}", customerId);
            return ResponseEntity.ok(CustomerOutput.from(updatedCustomer));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("addContractToCustomer failed for customerId {}: {}", customerId, e.getMessage());
            // Consider different HTTP status for different errors (e.g., 404 for customer not found vs 400 for bad input)
            return ResponseEntity.badRequest().body(e.getMessage()); // Or a proper error DTO with e.getMessage()
        } catch (Exception e) { // Catch unexpected errors
            LOGGER.error("Unexpected error in addContractToCustomer for customerId {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
