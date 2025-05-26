package klee.solution.bulille.pocs.blink.appserver.middle.services.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ContractInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.CustomerInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.SoldPrestationInput;
import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
// import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId; // Will use String for repository interaction
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.bson.types.ObjectId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerStorageOperations;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.ContractType;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerStorageOperations customerStorage;
    @Mock
    private PrestationRepository prestationRepository;

    @InjectMocks
    private CustomerService customerService;

    private CustomerId customerId;
    private Customer customer;
    private CustomerInput customerInput;
    private String customerIdString;
    private String salesSystemIdString;

    @BeforeEach
    void setUp() {
        customerIdString = UUID.randomUUID().toString();
        customerId = new CustomerId(customerIdString);
        salesSystemIdString = "prestation123";

        customer = new Customer();
        customer.id = new ObjectId(); // Use ObjectId
        customer.firstName = "John";
        customer.givenName = "Doe";
        customer.contracts = new ArrayList<>();

        customerInput = new CustomerInput("Jane", "Doe", LocalDate.of(1990, 1, 1)); // Corrected constructor
    }

    @Test
    void find_shouldCallStorageFind() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        Optional<Customer> result = this.customerService.find(customerId);
        assertTrue(result.isPresent());
        assertEquals(customer, result.get());
        verify(this.customerStorage).find(customerId);
    }

    @Test
    void searchCustomers_withNonEmptyQuery_shouldCallStorageFindByFirstNameOrGivenName() {
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> expectedDbPage = new PageImpl<>(Collections.singletonList(customer));
        when(this.customerStorage.findByFirstNameOrGivenNameContainingIgnoreCase(query, pageable)).thenReturn(expectedDbPage);

        Page<CustomerOutput> result = this.customerService.searchCustomers(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(CustomerOutput.from(customer).firstName(), result.getContent().get(0).firstName());
        verify(this.customerStorage).findByFirstNameOrGivenNameContainingIgnoreCase(query, pageable);
        verify(this.customerStorage, never()).findAll(pageable);
    }

    @Test
    void searchCustomers_withEmptyQuery_shouldCallStorageFindAll() {
        String query = " "; // Empty query
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> expectedDbPage = new PageImpl<>(Collections.singletonList(customer));
        when(this.customerStorage.findAll(pageable)).thenReturn(expectedDbPage);

        Page<CustomerOutput> result = this.customerService.searchCustomers(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(this.customerStorage).findAll(pageable);
        verify(this.customerStorage, never()).findByFirstNameOrGivenNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }
    
    @Test
    void createCustomer_validInput_shouldSaveAndReturnCustomer() {
        CustomerInput input = new CustomerInput("Test", "User", LocalDate.now()); // Corrected
        Customer expectedCustomer = new Customer();
        expectedCustomer.id = new ObjectId(); // ID will be ObjectId
        expectedCustomer.firstName = "Test";
        expectedCustomer.givenName = "User";
        expectedCustomer.dateOfBirth = LocalDate.now();
        expectedCustomer.contracts = new ArrayList<>();

        when(this.customerStorage.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.id = expectedCustomer.id; // Simulate ID assignment on save
            return c;
        });

        Customer result = this.customerService.createCustomer(input);

        assertNotNull(result);
        assertEquals(expectedCustomer.id, result.id);
        assertEquals(input.firstName(), result.firstName);
        verify(this.customerStorage).save(any(Customer.class));
    }

    @Test
    void createCustomer_missingFirstName_shouldThrowIllegalArgumentException() {
        CustomerInput input = new CustomerInput(null, "User", LocalDate.now()); // Corrected
        assertThrows(IllegalArgumentException.class, () -> this.customerService.createCustomer(input));
        verify(this.customerStorage, never()).save(any(Customer.class));
    }
    
    @Test
    void createCustomer_missingGivenName_shouldThrowIllegalArgumentException() {
        CustomerInput input = new CustomerInput("Test", null, LocalDate.now()); // Corrected
        assertThrows(IllegalArgumentException.class, () -> this.customerService.createCustomer(input));
        verify(this.customerStorage, never()).save(any(Customer.class));
    }

    @Test
    void addContract_customerNotFound_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.empty());
        ContractInput contractInput = new ContractInput(ContractType.PERMANENT, LocalDate.now(), null, Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }

    @Test
    void addContract_freeTrialMissingEndDate_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        ContractInput contractInput = new ContractInput(ContractType.FREE_TRIAL, LocalDate.now(), null, Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 10.0)));
        
        // Prestation mockPrestation = new Prestation(); // Unnecessary
        // mockPrestation.salesSystemId = salesSystemIdString; // Unnecessary
        // when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation)); // Unnecessary

        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }

    @Test
    void addContract_freeTrialTooLong_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1).plusDays(1); // More than one month
        ContractInput contractInput = new ContractInput(ContractType.FREE_TRIAL, startDate, endDate, Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 10.0)));
        
        // Prestation mockPrestation = new Prestation(); // Unnecessary
        // mockPrestation.salesSystemId = salesSystemIdString; // Unnecessary
        // when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation)); // Unnecessary
        
        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }
    
    @Test
    void addContract_missingStartDate_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        ContractInput contractInput = new ContractInput(ContractType.PERMANENT, null, LocalDate.now().plusYears(1), Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 10.0)));
        
        // No need to mock prestationRepository if validation fails before that
        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }
    
    @Test
    void addContract_nonPermanentMissingEndDate_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        // Using FREE_TRIAL as a valid non-PERMANENT type that requires an end date
        ContractInput contractInput = new ContractInput(ContractType.FREE_TRIAL, LocalDate.now(), null, Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 10.0)));
        
        // Prestation mockPrestation = new Prestation(); // Unnecessary
        // mockPrestation.salesSystemId = salesSystemIdString; // Unnecessary
        // when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation)); // Unnecessary

        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }

    @Test
    void addContract_startDateAfterEndDate_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.minusDays(1);
        // Using FREE_TRIAL as it requires an end date.
        ContractInput contractInput = new ContractInput(ContractType.FREE_TRIAL, startDate, endDate, Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 10.0)));

        // Prestation mockPrestation = new Prestation(); // Unnecessary
        // mockPrestation.salesSystemId = salesSystemIdString; // Unnecessary
        // when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation)); // Unnecessary
        
        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }
    
    @Test
    void addContract_overlappingContracts_shouldThrowIllegalArgumentException() {
        Contract existingContract = new Contract();
        existingContract.id = new ObjectId().toString(); 
        existingContract.type = ContractType.PERMANENT; 
        existingContract.start = LocalDate.now().minusMonths(1);
        existingContract.end = LocalDate.now().plusMonths(1); 
        customer.contracts.add(existingContract);

        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        
        LocalDate newContractStart = LocalDate.now(); 
        LocalDate newContractEnd = LocalDate.now().plusMonths(2);
        ContractInput contractInput = new ContractInput(ContractType.PERMANENT, newContractStart, newContractEnd, Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 10.0)));
        
        // Prestation mockPrestation = new Prestation(); // Unnecessary
        // mockPrestation.salesSystemId = salesSystemIdString; // Unnecessary
        // when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation)); // Unnecessary

        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }

    @Test
    void addContract_missingPrestations_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        ContractInput contractInput = new ContractInput(ContractType.PERMANENT, LocalDate.now(), null, Collections.emptyList()); // No prestations
        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }
    
    @Test
    void addContract_prestationNotFound_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        String nonExistentPrestationIdString = "nonexistent";
        ContractInput contractInput = new ContractInput(ContractType.PERMANENT, LocalDate.now(), null, Collections.singletonList(new SoldPrestationInput(nonExistentPrestationIdString, 1, 10.0)));
        
        when(this.prestationRepository.findById(nonExistentPrestationIdString)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }

    @Test
    void addContract_prestationUnitsNotPositive_shouldThrowIllegalArgumentException() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        ContractInput contractInput = new ContractInput(ContractType.PERMANENT, LocalDate.now(), null, Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 0, 10.0))); // Units = 0
        
        Prestation mockPrestation = new Prestation();
        mockPrestation.salesSystemId = salesSystemIdString;
        when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation));

        assertThrows(IllegalArgumentException.class, () -> this.customerService.addContract(customerId, contractInput));
    }

    @Test
    void addContract_validPermanentContract_shouldSaveAndReturnCustomer() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        when(this.customerStorage.save(any(Customer.class))).thenReturn(customer); 

        Prestation mockPrestation = new Prestation();
        mockPrestation.salesSystemId = salesSystemIdString;
        when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation));
        
        ContractInput contractInput = new ContractInput(
                ContractType.PERMANENT,
                LocalDate.now(),
                null, 
                Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 100.0))
        );

        Customer result = this.customerService.addContract(customerId, contractInput);

        assertNotNull(result);
        assertEquals(1, result.contracts.size());
        assertEquals(ContractType.PERMANENT, result.contracts.get(0).type);
        verify(this.customerStorage).save(customer);
    }
    
    @Test
    void addContract_validFreeTrialContract_shouldSaveAndReturnCustomer() {
        when(this.customerStorage.find(customerId)).thenReturn(Optional.of(customer));
        when(this.customerStorage.save(any(Customer.class))).thenReturn(customer);

        Prestation mockPrestation = new Prestation();
        mockPrestation.salesSystemId = salesSystemIdString;
        when(this.prestationRepository.findById(salesSystemIdString)).thenReturn(Optional.of(mockPrestation));
        
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(2); 

        ContractInput contractInput = new ContractInput(
                ContractType.FREE_TRIAL,
                startDate,
                endDate, 
                Collections.singletonList(new SoldPrestationInput(salesSystemIdString, 1, 0.0))
        );

        Customer result = this.customerService.addContract(customerId, contractInput);

        assertNotNull(result);
        assertEquals(1, result.contracts.size());
        Contract newContract = result.contracts.get(0);
        assertEquals(ContractType.FREE_TRIAL, newContract.type);
        assertEquals(startDate, newContract.start);
        assertEquals(endDate, newContract.end);
        verify(this.customerStorage).save(customer);
    }
}
