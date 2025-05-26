package klee.solution.bulille.pocs.blink.appserver.middle;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerStorage;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

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
// Ensure CustomerId is already imported or import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import java.time.LocalDate;
import java.util.stream.Collectors;


@Component
public class CustomerService {

    private final CustomerStorage customerStorage;
    private final PrestationRepository prestationRepository;

    public CustomerService(CustomerStorage customerStorage, PrestationRepository prestationRepository) {
        this.customerStorage = customerStorage;
        this.prestationRepository = prestationRepository;
    }

    public Optional<Customer> find(CustomerId customerId) {
        return this.customerStorage.find(customerId);
    }

    public Page<CustomerOutput> searchCustomers(String nameQuery, Pageable pageable) {
        Page<Customer> entityPages ;
        if (nameQuery == null || nameQuery.trim().isEmpty()) {
            entityPages = customerStorage.findAll(pageable); // Or handle as an error/empty page
        }else {
            entityPages = customerStorage.findByFirstNameOrGivenNameContainingIgnoreCase(nameQuery, pageable);
        }
        return new PageImpl<>(
                entityPages.getContent().stream().map(CustomerOutput::from).toList(),
                pageable,
                entityPages.getTotalElements()
        );

    }

    public Customer createCustomer(CustomerInput customerInput) {
        Customer customer = new Customer();
        customer.firstName = customerInput.firstName;
        customer.givenName = customerInput.givenName;
        customer.dateOfBirth = customerInput.dateOfBirth;
        customer.contracts = new ArrayList<>(); // Initialize empty contracts list

        // Potentially add more validation here based on customerInput
        if (customer.firstName == null || customer.firstName.trim().isEmpty() ||
                customer.givenName == null || customer.givenName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name and given name are required.");
        }
        return customerStorage.save(customer);
    }

    public Customer addContract(CustomerId customerId, ContractInput contractInput) {
        Customer customer = customerStorage.find(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId.id()));

        // Validate ContractInput
        if (contractInput.type == ContractType.FREE_TRIAL) {
            if (contractInput.endDate == null) {
                throw new IllegalArgumentException("End date is required for FREE_TRIAL contracts.");
            }
            if (contractInput.startDate != null && contractInput.endDate.isAfter(contractInput.startDate.plusMonths(1))) {
                throw new IllegalArgumentException("FREE_TRIAL contract end date cannot be more than one month after start date.");
            }
        } else if (contractInput.type == ContractType.PERMANENT) {
            // For PERMANENT contracts, endDate might be null or far in the future.
            // The DTO allows endDate to be null. If it's set, it will be used.
            // If it's null, the Contract object's endDate field will also be null.
        }

        if (contractInput.startDate == null) {
            throw new IllegalArgumentException("Contract start date is required.");
        }
        if (contractInput.type != ContractType.PERMANENT && contractInput.endDate == null) {
            throw new IllegalArgumentException("Contract end date is required for non-PERMANENT types.");
        }
        if (contractInput.endDate != null && contractInput.startDate.isAfter(contractInput.endDate)) {
            throw new IllegalArgumentException("Contract start date cannot be after end date.");
        }


        // Validate no overlapping contracts
        for (Contract existingContract : customer.contracts) {
            boolean overlaps = overlaps(contractInput, existingContract);
            if (overlaps) {
                throw new IllegalArgumentException("New contract dates overlap with an existing contract.");
            }
        }

        Contract newContract = new Contract(); // ID is auto-generated in Contract class
        newContract.type = contractInput.type;
        newContract.start = contractInput.startDate;
        newContract.end = (contractInput.type == ContractType.PERMANENT && contractInput.endDate == null) ? null : contractInput.endDate;

        if (contractInput.soldPrestations == null || contractInput.soldPrestations.isEmpty()) {
            throw new IllegalArgumentException("Contract must include at least one prestation.");
        }

        newContract.soldPrestations = contractInput.soldPrestations.stream().map(spInput -> {
            // Validate Prestation exists
            prestationRepository.findById(spInput.salesSystemId)
                    .orElseThrow(() -> new IllegalArgumentException("Prestation not found with salesSystemId: " + spInput.salesSystemId));

            SoldPrestation soldPrestation = new SoldPrestation();
            soldPrestation.salesSystemId = spInput.salesSystemId;
            soldPrestation.units = spInput.units;
            soldPrestation.totalBilledAmountForUnits = spInput.totalBilledAmountForUnits;
            if (soldPrestation.units <= 0) {
                throw new IllegalArgumentException("Units for a sold prestation must be positive.");
            }
            return soldPrestation;
        }).collect(Collectors.toList());

        if (customer.contracts == null) { // Should have been initialized in createCustomer
            customer.contracts = new ArrayList<>();
        }
        customer.contracts.add(newContract);
        return customerStorage.save(customer); // Save the updated customer object
    }

    private static boolean overlaps(ContractInput contractInput, Contract existingContract) {
        if (contractInput.endDate == null && existingContract.end == null && contractInput.type == ContractType.PERMANENT && existingContract.type == ContractType.PERMANENT) {
            // Two permanent contracts without end dates always overlap if their start dates allow any period of concurrent existence.
            // This simple check assumes a customer can't have two open-ended permanent contracts.
            // More sophisticated logic might be needed if permanent contracts can have specific start/end for different services.
            // For now, let's prevent two PERMANENT contracts if one is already active and has no end date.
            // This rule might need refinement based on business logic.
            if (existingContract.type == ContractType.PERMANENT && existingContract.end == null) {
                throw new IllegalArgumentException("An open-ended PERMANENT contract already exists.");
            }
        }

        // Define effective end dates for comparison (a very far date for null end dates)
        LocalDate existingContractEffectiveEnd = (existingContract.end == null) ? LocalDate.MAX : existingContract.end;
        LocalDate newContractEffectiveEnd = (contractInput.endDate == null && contractInput.type == ContractType.PERMANENT) ? LocalDate.MAX : contractInput.endDate;

        if (newContractEffectiveEnd == null) { // Should not happen if previous checks are okay for non-permanent
            throw new IllegalArgumentException("New contract effective end date is null unexpectedly.");
        }

        // Check for overlap: (StartA <= EndB) and (StartB <= EndA)
        boolean overlaps = !contractInput.startDate.isAfter(existingContractEffectiveEnd) &&
                !existingContract.start.isAfter(newContractEffectiveEnd);
        return overlaps;
    }
}
