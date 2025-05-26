package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Optional;

@Component
class CustomerStorage implements CustomerStorageOperations {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CustomerStorage.class);

    private final CustomerRepository customerRepository;

    public CustomerStorage(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("Attempting to find customer with id: {}", customerId.id());
        try {
            Optional<Customer> customer = this.customerRepository.findById(customerId.toOID());
            if (customer.isPresent()) {
                LOGGER.info("Successfully found customer with id: {}", customerId.id());
            } else {
                LOGGER.info("Customer not found with id: {}", customerId.id());
            }
            return customer;
        } catch (Exception e) {
            LOGGER.error("Error in find for customerId {}: {}", customerId.id(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Page<Customer> findByFirstNameOrGivenNameContainingIgnoreCase(@NonNull String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("Attempting to find customers by nameQuery: {}, page: {}, size: {}", nameQuery, pageable.getPageNumber(), pageable.getPageSize());
        try {
            // nameQuery is @NonNull, so no null check needed here. The existing check for trim().isEmpty() in service layer is still relevant if applicable there.
            Page<Customer> customers = this.customerRepository.findByFirstNameOrGivenNameContainingIgnoreCase(nameQuery, pageable);
            LOGGER.info("Successfully found {} customers by nameQuery: {}", customers.getTotalElements(), nameQuery);
            return customers;
        } catch (Exception e) {
            LOGGER.error("Error in findByFirstNameOrGivenNameContainingIgnoreCase with nameQuery {}: {}", nameQuery, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Page<Customer> findAll(@NonNull Pageable pageable) {
        LOGGER.info("Attempting to find all customers with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<Customer> customers = this.customerRepository.findAll(pageable);
            LOGGER.info("Successfully found {} customers (all).", customers.getTotalElements());
            return customers;
        } catch (Exception e) {
            LOGGER.error("Error in findAll: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Customer save(@NonNull Customer customer) {
        LOGGER.info("Attempting to save customer with current id (if any): {}", customer.id);
        try {
            Customer savedCustomer = this.customerRepository.save(customer);
            LOGGER.info("Successfully saved customer with id: {}", savedCustomer.id);
            return savedCustomer;
        } catch (Exception e) {
            LOGGER.error("Error in save for customer ID {}: {}", customer.id, e.getMessage(), e);
            throw e;
        }
    }
}
