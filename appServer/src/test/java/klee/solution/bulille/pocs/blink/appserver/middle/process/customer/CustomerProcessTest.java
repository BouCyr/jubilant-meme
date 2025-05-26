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
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.ContractType;
import org.bson.types.ObjectId; // Added
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

import java.time.LocalDate; // Added
import java.util.ArrayList; // Added
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProcessTest {

    @Mock
    private CustomerFinderService customerFinderService;
    @Mock
    private CustomerSearcherService customerSearcherService;
    @Mock
    private CustomerCreatorService customerCreatorService;
    @Mock
    private ContractAdderService contractAdderService;

    @InjectMocks
    private CustomerProcess customerProcess;

    private CustomerId customerId;
    private Customer customer; // This is the "global" customer object for setup
    private CustomerInput customerInput;
    private ContractInput contractInput;
    // private String customerIdString; // Not strictly needed if customerId object is used

    @BeforeEach
    void setUp() {
        String customerIdString = UUID.randomUUID().toString();
        customerId = new CustomerId(customerIdString);
        
        customer = new Customer(); 
        customer.id = new ObjectId(); // Initialize with a new ObjectId
        customer.firstName = "TestFirstName"; // Example name
        customer.givenName = "TestGivenName"; // Example name
        customer.contracts = new ArrayList<>();


        // Corrected CustomerInput constructor based on its definition (firstName, givenName, dateOfBirth)
        customerInput = new CustomerInput("John", "Doe", LocalDate.now()); 
        
        // Example for ContractInput, adjust as needed for specific tests
        contractInput = new ContractInput(ContractType.PERMANENT, LocalDate.now(), null, Collections.emptyList());
    }

    @Test
    void find_shouldCallCustomerFinderService() {
        Customer foundCustomer = new Customer(); // Create a separate customer for the mock return
        foundCustomer.id = new ObjectId(); 
        foundCustomer.firstName = "Found";
        foundCustomer.givenName = "User";
        
        when(this.customerFinderService.find(customerId)).thenReturn(Optional.of(foundCustomer));

        Optional<Customer> result = this.customerProcess.find(customerId);

        assertTrue(result.isPresent());
        assertEquals(foundCustomer, result.get());
        assertEquals(foundCustomer.id, result.get().id); // Verify ObjectId
        verify(this.customerFinderService).find(customerId);
    }

    @Test
    void searchCustomers_shouldCallCustomerSearcherService() {
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<CustomerOutput> expectedPage = new PageImpl<>(Collections.emptyList());
        when(this.customerSearcherService.searchCustomers(query, pageable)).thenReturn(expectedPage);

        Page<CustomerOutput> result = this.customerProcess.searchCustomers(query, pageable);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        verify(this.customerSearcherService).searchCustomers(query, pageable);
    }

    @Test
    void createCustomer_shouldCallCustomerCreatorService() {
        Customer createdCustomer = new Customer();
        createdCustomer.id = new ObjectId(); 
        createdCustomer.firstName = customerInput.firstName();
        createdCustomer.givenName = customerInput.givenName();
        // Note: CustomerProcess createCustomer doesn't set dateOfBirth itself, it passes input to service
        // So, the createdCustomer mock here just needs to reflect what the service would return.

        when(this.customerCreatorService.createCustomer(customerInput)).thenReturn(createdCustomer);

        Customer result = this.customerProcess.createCustomer(customerInput);

        assertNotNull(result);
        assertEquals(createdCustomer, result);
        assertEquals(createdCustomer.id, result.id); // Verify ObjectId
        verify(this.customerCreatorService).createCustomer(customerInput);
    }

    @Test
    void addContract_shouldCallContractAdderService() {
        Customer customerWithContract = new Customer();
        // Use a new ObjectId for the customer returned by the service, or the one from setUp if that's intended.
        // For clarity, let's assume it's the same customer being updated.
        customerWithContract.id = new ObjectId(this.customer.id.toByteArray()); 
        customerWithContract.firstName = this.customer.firstName;
        customerWithContract.givenName = this.customer.givenName;
        
        Contract newContract = new Contract();
        newContract.id = new ObjectId().toString(); // Contract ID is String
        newContract.type = contractInput.type();
        // Simulate the service adding the contract
        customerWithContract.contracts = new ArrayList<>(Collections.singletonList(newContract));


        when(this.contractAdderService.addContract(customerId, contractInput)).thenReturn(customerWithContract);

        Customer result = this.customerProcess.addContract(customerId, contractInput);

        assertNotNull(result);
        assertEquals(customerWithContract, result); // Check if the returned object is the one from the mock
        assertFalse(result.contracts.isEmpty());
        assertEquals(newContract.id, result.contracts.get(0).id);
        verify(this.contractAdderService).addContract(customerId, contractInput);
    }
}
