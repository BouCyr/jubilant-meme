package klee.solution.bulille.pocs.blink.appserver.in.http;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.CustomerService;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
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

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOutput> getCustomer(@PathVariable("id") String id){

        var customerId = new CustomerId(id);
        Optional<Customer> customer = this.customerService.find(customerId);


        return customer
                .map(CustomerOutput::from)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerOutput>> searchCustomers(
            @RequestParam(name = "name", required = false) String nameQuery,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerOutput> customers = customerService.searchCustomers(nameQuery, pageable);

        return ResponseEntity.ok(customers);
    }

    @PostMapping
    public ResponseEntity<CustomerOutput> createCustomer(@RequestBody CustomerInput customerInput) {
        // Basic validation example (can be enhanced with @Valid and validation annotations on DTO)
        if (customerInput == null || customerInput.firstName == null || customerInput.firstName.trim().isEmpty() ||
            customerInput.givenName == null || customerInput.givenName.trim().isEmpty()) {
            // Consider logging this attempt
            return ResponseEntity.badRequest().build(); // Or throw a custom exception
        }
        try {
            Customer createdCustomer = customerService.createCustomer(customerInput);
            return ResponseEntity.status(HttpStatus.CREATED).body(CustomerOutput.from(createdCustomer));
        } catch (IllegalArgumentException e) {
            // Log the exception e.getMessage()
            return ResponseEntity.badRequest().build(); // Or a more specific error response
        }
    }

    @PostMapping("/{customerId}/contracts")
    public ResponseEntity<?> addContractToCustomer(
            @PathVariable String customerId,
            @RequestBody ContractInput contractInput) {
        try {
            Customer updatedCustomer = customerService.addContract(new CustomerId(customerId), contractInput);
            return ResponseEntity.ok(CustomerOutput.from(updatedCustomer));
        } catch (IllegalArgumentException e) {
            // Log e.getMessage()
            // Consider different HTTP status for different errors (e.g., 404 for customer not found vs 400 for bad input)
            return ResponseEntity.badRequest().body(e.getMessage()); // Or a proper error DTO with e.getMessage()
        } catch (Exception e) { // Catch unexpected errors
            // Log e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
