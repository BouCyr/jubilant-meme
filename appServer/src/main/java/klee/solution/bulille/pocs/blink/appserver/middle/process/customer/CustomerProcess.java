package klee.solution.bulille.pocs.blink.appserver.middle.process.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.services.customer.ContractAdderService;
import klee.solution.bulille.pocs.blink.appserver.middle.services.customer.CustomerCreatorService;
import klee.solution.bulille.pocs.blink.appserver.middle.services.customer.CustomerFinderService;
import klee.solution.bulille.pocs.blink.appserver.middle.services.customer.CustomerSearcherService;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.Optional;

@Component
class CustomerProcess implements CustomerFinder, CustomerSearcher, CustomerCreator, ContractAdder {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CustomerProcess.class);

    private final CustomerFinderService customerFinderService;
    private final CustomerSearcherService customerSearcherService;
    private final CustomerCreatorService customerCreatorService;
    private final ContractAdderService contractAdderService;

    public CustomerProcess(
            CustomerFinderService customerFinderService,
            CustomerSearcherService customerSearcherService,
            CustomerCreatorService customerCreatorService,
            ContractAdderService contractAdderService) {
        this.customerFinderService = customerFinderService;
        this.customerSearcherService = customerSearcherService;
        this.customerCreatorService = customerCreatorService;
        this.contractAdderService = contractAdderService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("find called in CustomerProcess for customerId: {}", customerId.id());
        Optional<Customer> customer = this.customerFinderService.find(customerId);
        LOGGER.info("find successfully completed in CustomerProcess for customerId: {}. Customer found: {}", customerId.id(), customer.isPresent());
        return customer;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerOutput> searchCustomers(@NonNull String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("searchCustomers called in CustomerProcess with nameQuery: {}, page: {}, size: {}", nameQuery, pageable.getPageNumber(), pageable.getPageSize());
        Page<CustomerOutput> customers = this.customerSearcherService.searchCustomers(nameQuery, pageable);
        LOGGER.info("searchCustomers successfully completed in CustomerProcess. Found {} customers.", customers.getTotalElements());
        return customers;
    }

    @Override
    @Transactional
    public Customer createCustomer(@NonNull CustomerInput customerInput) {
        LOGGER.info("createCustomer called in CustomerProcess for customer: {} {}", customerInput.firstName(), customerInput.givenName());
        Customer customer = this.customerCreatorService.createCustomer(customerInput);
        LOGGER.info("createCustomer successfully completed in CustomerProcess. Customer ID: {}", customer.id);
        return customer;
    }

    @Override
    @Transactional
    public Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput) {
        LOGGER.info("addContract called in CustomerProcess for customerId: {} with contract type: {}", customerId.id(), contractInput.type());
        Customer customer = this.contractAdderService.addContract(customerId, contractInput);
        LOGGER.info("addContract successfully completed in CustomerProcess for customerId: {}. New contract ID: {}", customerId.id(), customer.contracts.getLast().id); // Assuming new contract is last
        return customer;
    }
}
