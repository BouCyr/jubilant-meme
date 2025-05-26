package klee.solution.bulille.pocs.blink.appserver.middle;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerStorageOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import java.util.ArrayList;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.ContractType;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.SoldPrestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;
import java.time.LocalDate;
import java.util.stream.Collectors;


@Component
class CustomerService implements CustomerServicing {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CustomerService.class);

    private final CustomerStorageOperations customerStorage;
    private final PrestationRepository prestationRepository;

    public CustomerService(CustomerStorageOperations customerStorage, PrestationRepository prestationRepository) {
        this.customerStorage = customerStorage;
        this.prestationRepository = prestationRepository;
    }

    @Override
    public Optional<Customer> find(@NonNull CustomerId customerId) {
        LOGGER.info("find called with customerId: {}", customerId.id());
        try {
            Optional<Customer> customer = this.customerStorage.find(customerId);
            if (customer.isPresent()) {
                LOGGER.info("find completed successfully for customerId: {}. Customer found.", customerId.id());
            } else {
                LOGGER.info("find completed successfully for customerId: {}. Customer not found.", customerId.id());
            }
            return customer;
        } catch (Exception e) {
            LOGGER.error("Error in find for customerId {}: {}", customerId.id(), e.getMessage(), e);
            throw e; 
        }
    }

    @Override
    public Page<CustomerOutput> searchCustomers(@NonNull String nameQuery, @NonNull Pageable pageable) {
        LOGGER.info("searchCustomers called with nameQuery: {}, page: {}, size: {}", nameQuery, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<Customer> entityPages;
            // nameQuery is @NonNull, so no null check needed here. Check for empty after trim.
            if (nameQuery.trim().isEmpty()) {
                LOGGER.info("searchCustomers: nameQuery is empty, finding all customers.");
                entityPages = this.customerStorage.findAll(pageable);
            } else {
                LOGGER.info("searchCustomers: nameQuery is '{}', searching by name.", nameQuery);
                entityPages = this.customerStorage.findByFirstNameOrGivenNameContainingIgnoreCase(nameQuery, pageable);
            }
            Page<CustomerOutput> customerOutputs = new PageImpl<>(
                    entityPages.getContent().stream().map(CustomerOutput::from).toList(),
                    pageable,
                    entityPages.getTotalElements()
            );
            LOGGER.info("searchCustomers completed successfully. Returning {} customers.", customerOutputs.getTotalElements());
            return customerOutputs;
        } catch (Exception e) {
            LOGGER.error("Error in searchCustomers with nameQuery {}: {}", nameQuery, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Customer createCustomer(@NonNull CustomerInput customerInput) {
        LOGGER.info("createCustomer called for customer: {} {}", customerInput.firstName(), customerInput.givenName());
        try {
            Customer customer = new Customer();
            customer.firstName = customerInput.firstName();
            customer.givenName = customerInput.givenName();
            customer.dateOfBirth = customerInput.dateOfBirth();
            customer.contracts = new ArrayList<>();

            if (customer.firstName == null || customer.firstName.trim().isEmpty() ||
                    customer.givenName == null || customer.givenName.trim().isEmpty()) {
                LOGGER.warn("createCustomer failed: First name and given name are required. Input: {}", customerInput);
                throw new IllegalArgumentException("First name and given name are required.");
            }
            LOGGER.info("Attempting to save new customer: {} {}", customer.firstName, customer.givenName);
            Customer savedCustomer = this.customerStorage.save(customer);
            LOGGER.info("Successfully saved new customer with ID: {}", savedCustomer.getId());
            LOGGER.info("createCustomer completed successfully. Customer ID: {}", savedCustomer.getId());
            return savedCustomer;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in createCustomer for {} {}: {}", customerInput.firstName(), customerInput.givenName(), e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while creating customer.", e);
        }
    }

    @Override
    public Customer addContract(@NonNull CustomerId customerId, @NonNull ContractInput contractInput) {
        LOGGER.info("addContract called for customerId: {} with contract type: {}", customerId.id(), contractInput.type());
        try {
            LOGGER.info("Fetching customer with ID: {} to add contract", customerId.id());
            Customer customer = this.customerStorage.find(customerId)
                    .orElseThrow(() -> {
                        LOGGER.warn("addContract failed: Customer not found with ID: {}", customerId.id());
                        return new IllegalArgumentException("Customer not found with ID: " + customerId.id());
                    });

            if (contractInput.type() == ContractType.FREE_TRIAL) {
                if (contractInput.endDate() == null) {
                    LOGGER.warn("addContract failed for customerId {}: End date is required for FREE_TRIAL contracts.", customerId.id());
                    throw new IllegalArgumentException("End date is required for FREE_TRIAL contracts.");
                }
                if (contractInput.startDate() != null && contractInput.endDate().isAfter(contractInput.startDate().plusMonths(1))) {
                    LOGGER.warn("addContract failed for customerId {}: FREE_TRIAL contract end date cannot be more than one month after start date.", customerId.id());
                    throw new IllegalArgumentException("FREE_TRIAL contract end date cannot be more than one month after start date.");
                }
            }

            if (contractInput.startDate() == null) {
                LOGGER.warn("addContract failed for customerId {}: Contract start date is required.", customerId.id());
                throw new IllegalArgumentException("Contract start date is required.");
            }
            if (contractInput.type() != ContractType.PERMANENT && contractInput.endDate() == null) {
                LOGGER.warn("addContract failed for customerId {}: Contract end date is required for non-PERMANENT types.", customerId.id());
                throw new IllegalArgumentException("Contract end date is required for non-PERMANENT types.");
            }
            if (contractInput.endDate() != null && contractInput.startDate().isAfter(contractInput.endDate())) {
                LOGGER.warn("addContract failed for customerId {}: Contract start date cannot be after end date.", customerId.id());
                throw new IllegalArgumentException("Contract start date cannot be after end date.");
            }

            for (Contract existingContract : customer.contracts) {
                if (CustomerService.overlaps(contractInput, existingContract)) {
                    LOGGER.warn("addContract failed for customerId {}: New contract dates overlap with an existing contract (ID: {}).", customerId.id(), existingContract.id);
                    throw new IllegalArgumentException("New contract dates overlap with an existing contract.");
                }
            }

            Contract newContract = new Contract();
            newContract.type = contractInput.type();
            newContract.start = contractInput.startDate();
            newContract.end = (contractInput.type() == ContractType.PERMANENT && contractInput.endDate() == null) ? null : contractInput.endDate();

            if (contractInput.soldPrestations() == null || contractInput.soldPrestations().isEmpty()) {
                LOGGER.warn("addContract failed for customerId {}: Contract must include at least one prestation.", customerId.id());
                throw new IllegalArgumentException("Contract must include at least one prestation.");
            }

            newContract.soldPrestations = contractInput.soldPrestations().stream().map(spInput -> {
                LOGGER.info("Fetching prestation with salesSystemId: {} for contract", spInput.salesSystemId());
                this.prestationRepository.findById(spInput.salesSystemId())
                        .orElseThrow(() -> {
                            LOGGER.warn("addContract failed for customerId {}: Prestation not found with salesSystemId: {}", customerId.id(), spInput.salesSystemId());
                            return new IllegalArgumentException("Prestation not found with salesSystemId: " + spInput.salesSystemId());
                        });

                SoldPrestation soldPrestation = new SoldPrestation();
                soldPrestation.salesSystemId = spInput.salesSystemId();
                soldPrestation.units = spInput.units();
                soldPrestation.totalBilledAmountForUnits = spInput.totalBilledAmountForUnits();
                if (soldPrestation.units <= 0) {
                    LOGGER.warn("addContract failed for customerId {}: Units for a sold prestation (salesSystemId: {}) must be positive. Received: {}", customerId.id(), spInput.salesSystemId(), spInput.units());
                    throw new IllegalArgumentException("Units for a sold prestation must be positive.");
                }
                return soldPrestation;
            }).collect(Collectors.toList());

            if (customer.contracts == null) {
                customer.contracts = new ArrayList<>();
            }
            customer.contracts.add(newContract);
            LOGGER.info("Attempting to save customer {} with new contract {}", customerId.id(), newContract.id);
            Customer updatedCustomer = this.customerStorage.save(customer);
            LOGGER.info("Successfully saved customer {} with new contract {}", customerId.id(), newContract.id);
            LOGGER.info("addContract completed successfully for customerId: {}. New contract ID: {}", customerId.id(), newContract.id);
            return updatedCustomer;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in addContract for customerId {}: {}", customerId.id(), e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while adding contract.", e);
        }
    }

    private static boolean overlaps(ContractInput contractInput, Contract existingContract) {
        if (contractInput.endDate() == null && existingContract.end == null && contractInput.type() == ContractType.PERMANENT && existingContract.type == ContractType.PERMANENT) {
            if (existingContract.type == ContractType.PERMANENT && existingContract.end == null) {
                 LOGGER.warn("Overlap check: An open-ended PERMANENT contract already exists (ID: {}).", existingContract.id);
                throw new IllegalArgumentException("An open-ended PERMANENT contract already exists.");
            }
        }

        LocalDate existingContractEffectiveEnd = (existingContract.end == null) ? LocalDate.MAX : existingContract.end;
        LocalDate newContractEffectiveEnd = (contractInput.endDate() == null && contractInput.type() == ContractType.PERMANENT) ? LocalDate.MAX : contractInput.endDate();

        if (newContractEffectiveEnd == null) {
            LOGGER.error("Overlap check: New contract effective end date is null unexpectedly for contract type {}", contractInput.type());
            throw new IllegalArgumentException("New contract effective end date is null unexpectedly.");
        }

        boolean overlaps = !contractInput.startDate().isAfter(existingContractEffectiveEnd) &&
                !existingContract.start.isAfter(newContractEffectiveEnd);
        if (overlaps) {
            LOGGER.info("Overlap detected between new contract (start: {}, end: {}) and existing contract (ID: {}, start: {}, end: {})",
                    contractInput.startDate(), newContractEffectiveEnd, existingContract.id, existingContract.start, existingContractEffectiveEnd);
        }
        return overlaps;
    }
}
