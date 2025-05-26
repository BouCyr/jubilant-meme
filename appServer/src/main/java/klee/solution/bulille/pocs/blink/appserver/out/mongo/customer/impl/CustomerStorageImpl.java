package klee.solution.bulille.pocs.blink.appserver.out.mongo.customer.impl;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.customer.CustomerStorageApi;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component; // Or @Repository

import java.util.Optional;

@Component // Using @Component as per original, @Repository would also be appropriate
class CustomerStorageImpl implements CustomerStorageApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerStorageImpl.class);
    private final CustomerRepository customerRepository;

    public CustomerStorageImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("CustomerStorage.find called with customerId: {}", customerId);
        Optional<Customer> result = this.customerRepository.findById(customerId.toOID());
        LOGGER.info("CustomerStorage.find finished for customerId: {}", customerId);
        return result;
    }

    @Override
    public Page<Customer> findByFirstNameOrGivenNameContainingIgnoreCase(String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("CustomerStorage.findByFirstNameOrGivenNameContainingIgnoreCase called with nameQuery: {}, pageable: {}", nameQuery, pageable);
        Page<Customer> result = this.customerRepository.findByFirstNameOrGivenNameContainingIgnoreCase(nameQuery, pageable);
        LOGGER.info("CustomerStorage.findByFirstNameOrGivenNameContainingIgnoreCase finished, found {} customers", result.getTotalElements());
        return result;
    }

    @Override
    @NonNull
    public Page<Customer> findAll(@NonNull Pageable pageable) {
        LOGGER.info("CustomerStorage.findAll called with pageable: {}", pageable);
        Page<Customer> result = this.customerRepository.findAll(pageable);
        LOGGER.info("CustomerStorage.findAll finished, found {} customers", result.getTotalElements());
        return result;
    }

    @Override
    @NonNull
    public Customer save(@NonNull Customer customer) {
        LOGGER.info("CustomerStorage.save called for customer: {}", customer.id); // Assuming customer has an id or relevant field for logging
        Customer savedCustomer = this.customerRepository.save(customer);
        LOGGER.info("CustomerStorage.save finished for customer: {}", savedCustomer.id);
        return savedCustomer;
    }
}
