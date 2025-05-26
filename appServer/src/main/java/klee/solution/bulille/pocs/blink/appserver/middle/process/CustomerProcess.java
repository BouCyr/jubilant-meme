package klee.solution.bulille.pocs.blink.appserver.middle.process;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.CustomerServicing;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.Optional;

@Component
class CustomerProcess implements CustomerProcessing {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CustomerProcess.class);

    private final CustomerServicing customerService;

    public CustomerProcess(CustomerServicing customerService) {
        this.customerService = customerService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("find called in CustomerProcess for customerId: {}", customerId.id());
        Optional<Customer> customer = this.customerService.find(customerId);
        LOGGER.info("find successfully completed in CustomerProcess for customerId: {}. Customer found: {}", customerId.id(), customer.isPresent());
        return customer;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOutput> searchCustomers(@NonNull String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("searchCustomers called in CustomerProcess with nameQuery: {}, page: {}, size: {}", nameQuery, pageable.getPageNumber(), pageable.getPageSize());
        Page<CustomerOutput> customers = this.customerService.searchCustomers(nameQuery, pageable);
        LOGGER.info("searchCustomers successfully completed in CustomerProcess. Found {} customers.", customers.getTotalElements());
        return customers;
    }

    @Override
    @Transactional
    public Customer createCustomer(@NonNull CustomerInput customerInput) {
        LOGGER.info("createCustomer called in CustomerProcess for customer: {} {}", customerInput.firstName(), customerInput.givenName());
        Customer customer = this.customerService.createCustomer(customerInput);
        LOGGER.info("createCustomer successfully completed in CustomerProcess. Customer ID: {}", customer.getId());
        return customer;
    }

    @Override
    @Transactional
    public Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput) {
        LOGGER.info("addContract called in CustomerProcess for customerId: {} with contract type: {}", customerId.id(), contractInput.type());
        Customer customer = this.customerService.addContract(customerId, contractInput);
        LOGGER.info("addContract successfully completed in CustomerProcess for customerId: {}. New contract ID: {}", customerId.id(), customer.getContracts().get(customer.getContracts().size()-1).id); // Assuming new contract is last
        return customer;
    }
}
