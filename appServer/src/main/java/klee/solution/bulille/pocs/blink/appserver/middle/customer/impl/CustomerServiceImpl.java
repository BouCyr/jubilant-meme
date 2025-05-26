package klee.solution.bulille.pocs.blink.appserver.middle.customer.impl;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.customer.CustomerServiceApi;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.customer.CustomerStorageApi; // New import
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.ContractType;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
// import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerStorage; // Old import
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.SoldPrestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service; // Changed from @Component to @Service

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service // Changed from @Component to @Service
class CustomerServiceImpl implements CustomerServiceApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServiceImpl.class); // Updated class reference

    private final CustomerStorageApi customerStorageApi; // Changed type to CustomerStorageApi
    private final PrestationRepository prestationRepository;

    public CustomerServiceImpl(CustomerStorageApi customerStorageApi, PrestationRepository prestationRepository) { // Changed type in constructor
        this.customerStorageApi = customerStorageApi;
        this.prestationRepository = prestationRepository;
    }

    @Override
    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("find called with customerId: {}", customerId);
        Optional<Customer> customer = this.customerStorageApi.find(customerId); // Use customerStorageApi
        if (customer.isPresent()) {
            LOGGER.info("Customer found with id: {}", customerId);
        } else {
            LOGGER.warn("Customer not found with id: {}", customerId);
        }
        return customer;
    }

    @Override
    // nameQuery can be null or empty, so it's not annotated with @NonNull
    public Page<CustomerOutput> searchCustomers(String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("searchCustomers called with nameQuery: {}, pageable: {}", nameQuery, pageable);
        Page<Customer> entityPages;
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            LOGGER.info("nameQuery is empty, searching all customers.");
            entityPages = this.customerStorageApi.findAll(pageable); // Use customerStorageApi
        } else {
            LOGGER.info("Searching customers with nameQuery: {}", nameQuery);
            entityPages = this.customerStorageApi.findByFirstNameOrGivenNameContainingIgnoreCase(nameQuery, pageable); // Use customerStorageApi
        }
        LOGGER.info("searchCustomers found {} customers", entityPages.getTotalElements());
        return new PageImpl<>(
                entityPages.getContent().stream().map(CustomerOutput::from).toList(),
                pageable,
                entityPages.getTotalElements()
        );
    }

    @Override
    @NonNull
    public Customer createCustomer(@NonNull CustomerInput customerInput) {
        LOGGER.info("createCustomer called with customerInput: {}", customerInput);
        if (customerInput.firstName() == null || customerInput.firstName().trim().isEmpty() ||
                customerInput.givenName() == null || customerInput.givenName().trim().isEmpty()) {
            LOGGER.warn("Attempted to create customer with invalid data: first name or given name missing. Input: {}", customerInput);
            throw new IllegalArgumentException("First name and given name are required.");
        }

        Customer customer = new Customer();
        customer.firstName = customerInput.firstName();
        customer.givenName = customerInput.givenName();
        customer.dateOfBirth = customerInput.dateOfBirth();
        customer.contracts = new ArrayList<>(); // Initialize empty contracts list

        try {
            Customer savedCustomer = this.customerStorageApi.save(customer); // Use customerStorageApi
            LOGGER.info("Customer created successfully with id: {}", savedCustomer.id());
            return savedCustomer;
        } catch (Exception e) {
            LOGGER.error("Error saving customer: {}", e.getMessage(), e);
            throw e; // Re-throw the original exception or a custom one
        }
    }

    @Override
    @NonNull
    public Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput) {
        LOGGER.info("addContract called for customerId: {} with contractInput: {}", customerId, contractInput);
        Customer customer = this.customerStorageApi.find(customerId) // Use customerStorageApi
                .orElseThrow(() -> {
                    LOGGER.warn("Customer not found with ID: {} when trying to add contract.", customerId.id());
                    return new CustomerNotFoundException("Customer not found with ID: " + customerId.id());
                });
        LOGGER.info("Found customer {} for adding contract.", customerId.id());

        // Validate ContractInput
        if (contractInput.type() == ContractType.FREE_TRIAL) {
            if (contractInput.endDate() == null) {
                LOGGER.warn("End date is required for FREE_TRIAL contracts. CustomerId: {}, Input: {}", customerId.id(), contractInput);
                throw new IllegalArgumentException("End date is required for FREE_TRIAL contracts.");
            }
            if (contractInput.startDate() != null && contractInput.endDate().isAfter(contractInput.startDate().plusMonths(1))) {
                LOGGER.warn("FREE_TRIAL contract end date cannot be more than one month after start date. CustomerId: {}, Input: {}", customerId.id(), contractInput);
                throw new IllegalArgumentException("FREE_TRIAL contract end date cannot be more than one month after start date.");
            }
        } else if (contractInput.type() == ContractType.PERMANENT) {
            LOGGER.info("Processing PERMANENT contract type for customerId: {}", customerId.id());
        }

        if (contractInput.startDate() == null) {
            LOGGER.warn("Contract start date is required. CustomerId: {}, Input: {}", customerId.id(), contractInput);
            throw new IllegalArgumentException("Contract start date is required.");
        }
        if (contractInput.type() != ContractType.PERMANENT && contractInput.endDate() == null) {
            LOGGER.warn("Contract end date is required for non-PERMANENT types. CustomerId: {}, Input: {}", customerId.id(), contractInput);
            throw new IllegalArgumentException("Contract end date is required for non-PERMANENT types.");
        }
        if (contractInput.endDate() != null && contractInput.startDate().isAfter(contractInput.endDate())) {
            LOGGER.warn("Contract start date cannot be after end date. CustomerId: {}, Input: {}", customerId.id(), contractInput);
            throw new IllegalArgumentException("Contract start date cannot be after end date.");
        }
        LOGGER.debug("Basic contract date validations passed for customerId: {}", customerId.id());


        for (Contract existingContract : customer.contracts) {
            boolean overlaps = overlaps(contractInput, existingContract);
            if (overlaps) {
                LOGGER.warn("New contract dates overlap with an existing contract. CustomerId: {}, NewContractInput: {}, ExistingContract: {}", customerId.id(), contractInput, existingContract);
                throw new IllegalArgumentException("New contract dates overlap with an existing contract.");
            }
        }
        LOGGER.debug("Overlap validation passed for customerId: {}", customerId.id());

        Contract newContract = new Contract();
        newContract.type = contractInput.type();
        newContract.start = contractInput.startDate();
        newContract.end = (contractInput.type() == ContractType.PERMANENT && contractInput.endDate() == null) ? null : contractInput.endDate();

        if (contractInput.soldPrestations() == null || contractInput.soldPrestations().isEmpty()) {
            LOGGER.warn("Contract must include at least one prestation. CustomerId: {}, Input: {}", customerId.id(), contractInput);
            throw new IllegalArgumentException("Contract must include at least one prestation.");
        }
        LOGGER.debug("Prestation presence validation passed for customerId: {}", customerId.id());

        newContract.soldPrestations = contractInput.soldPrestations().stream().map(spInput -> {
            LOGGER.debug("Processing sold prestation input: {} for customerId: {}", spInput, customerId.id());
            this.prestationRepository.findById(spInput.salesSystemId())
                    .orElseThrow(() -> {
                        LOGGER.warn("Prestation not found with salesSystemId: {} for customerId: {}", spInput.salesSystemId(), customerId.id());
                        return new IllegalArgumentException("Prestation not found with salesSystemId: " + spInput.salesSystemId());
                    });

            SoldPrestation soldPrestation = new SoldPrestation();
            soldPrestation.salesSystemId = spInput.salesSystemId();
            soldPrestation.units = spInput.units();
            soldPrestation.totalBilledAmountForUnits = spInput.totalBilledAmountForUnits();
            if (soldPrestation.units <= 0) {
                LOGGER.warn("Units for a sold prestation must be positive. CustomerId: {}, PrestationInput: {}", customerId.id(), spInput);
                throw new IllegalArgumentException("Units for a sold prestation must be positive.");
            }
            LOGGER.debug("Sold prestation created: {} for customerId: {}", soldPrestation, customerId.id());
            return soldPrestation;
        }).collect(Collectors.toList());

        if (customer.contracts == null) {
            LOGGER.warn("Customer contracts list was null, initializing. This should not happen if createCustomer initializes it. CustomerId: {}", customerId.id());
            customer.contracts = new ArrayList<>();
        }
        customer.contracts.add(newContract);
        try {
            Customer updatedCustomer = this.customerStorageApi.save(customer); // Use customerStorageApi
            LOGGER.info("Contract added successfully to customer: {}. New contract ID: {}", customerId.id(), newContract.id);
            return updatedCustomer;
        } catch (Exception e) {
            LOGGER.error("Error saving customer after adding contract for customerId {}: {}", customerId.id(), e.getMessage(), e);
            throw e;
        }
    }

    private static boolean overlaps(@NonNull ContractInput contractInput, @NonNull Contract existingContract) {
        if (contractInput.endDate() == null && existingContract.end == null && contractInput.type() == ContractType.PERMANENT && existingContract.type == ContractType.PERMANENT) {
            if (existingContract.type == ContractType.PERMANENT && existingContract.end == null) {
                throw new IllegalArgumentException("An open-ended PERMANENT contract already exists.");
            }
        }

        LocalDate existingContractEffectiveEnd = (existingContract.end == null) ? LocalDate.MAX : existingContract.end;
        LocalDate newContractEffectiveEnd = (contractInput.endDate() == null && contractInput.type() == ContractType.PERMANENT) ? LocalDate.MAX : contractInput.endDate();

        if (newContractEffectiveEnd == null) {
            throw new IllegalArgumentException("New contract effective end date is null unexpectedly.");
        }

        boolean overlaps = !contractInput.startDate().isAfter(existingContractEffectiveEnd) &&
                !existingContract.start.isAfter(newContractEffectiveEnd);
        return overlaps;
    }

    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(String message) {
            super(message);
        }
    }
}
