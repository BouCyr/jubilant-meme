package klee.solution.bulille.pocs.blink.appserver.in.http;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.process.customer.ContractAdder;
import klee.solution.bulille.pocs.blink.appserver.middle.process.customer.CustomerCreator;
import klee.solution.bulille.pocs.blink.appserver.middle.process.customer.CustomerFinder;
import klee.solution.bulille.pocs.blink.appserver.middle.process.customer.CustomerSearcher;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CustomerController.class);

    private final CustomerFinder customerFinder;
    private final CustomerSearcher customerSearcher;
    private final CustomerCreator customerCreator;
    private final ContractAdder contractAdder;

    public CustomerController(CustomerFinder customerFinder, CustomerSearcher customerSearcher, CustomerCreator customerCreator, ContractAdder contractAdder) {
        this.customerFinder = customerFinder;
        this.customerSearcher = customerSearcher;
        this.customerCreator = customerCreator;
        this.contractAdder = contractAdder;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOutput> getCustomer(@PathVariable("id") String id){
        LOGGER.info("getCustomer called with id: {}", id);
        try {
            var customerId = new CustomerId(id);
            Optional<Customer> customer = this.customerFinder.find(customerId);

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
            Page<CustomerOutput> customers = this.customerSearcher.searchCustomers(nameQuery, pageable);
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
            if (customerInput.firstName() == null || customerInput.firstName().trim().isEmpty() || customerInput.givenName() == null || customerInput.givenName().trim().isEmpty()) {
                LOGGER.warn("createCustomer failed due to missing required fields: {}", customerInput);
                return ResponseEntity.badRequest().build(); // Or throw a custom exception
            }
            Customer createdCustomer = this.customerCreator.createCustomer(customerInput);
            LOGGER.info("createCustomer completed successfully for customerId: {}", createdCustomer.id);
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
            Customer updatedCustomer = this.contractAdder.addContract(new CustomerId(customerId), contractInput);
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
