package klee.solution.bulille.pocs.blink.appserver.middle;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.ActivityInput;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.ActivityRepository;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerRepository;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.SoldPrestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;
// import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId; // Not directly used here as customerId is String

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 

import java.time.LocalDate;
import java.util.List;
// import java.util.Optional; // Not directly used here

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final CustomerRepository customerRepository; 
    private final PrestationRepository prestationRepository; 

    public ActivityService(ActivityRepository activityRepository,
                           CustomerRepository customerRepository,
                           PrestationRepository prestationRepository) {
        this.activityRepository = activityRepository;
        this.customerRepository = customerRepository;
        this.prestationRepository = prestationRepository;
    }

    @Transactional 
    public Activity addActivity(ActivityInput activityInput) {
        if (activityInput.unitsConsumed <= 0) {
            throw new IllegalArgumentException("Units consumed must be positive.");
        }
        if (activityInput.doneOn == null ) {
            throw new IllegalArgumentException("Activity date (doneOn) is required.");
        }
        if (activityInput.doneOn.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Activity date cannot be in the future.");
        }

        ObjectId customerOid = new ObjectId(activityInput.customerId);
        Customer customer = customerRepository.findById(customerOid)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + activityInput.customerId));

        Contract currentContract = customer.contracts.stream()
            .filter(c -> c.id.equals(activityInput.contractId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Contract not found with ID: " + activityInput.contractId + " for customer " + activityInput.customerId));

        if (currentContract.start.isAfter(activityInput.doneOn) || 
            (currentContract.end != null && currentContract.end.isBefore(activityInput.doneOn))) {
            throw new IllegalArgumentException("Activity date " + activityInput.doneOn + " is not within the contract period (" + currentContract.start + " - " + (currentContract.end == null ? "Permanent" : currentContract.end) + ").");
        }

        SoldPrestation soldPrestationInContract = currentContract.soldPrestations.stream()
            .filter(sp -> sp.salesSystemId.equals(activityInput.salesSystemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Prestation with salesSystemId: " + activityInput.salesSystemId + " not found in contract " + currentContract.id));

        // The repository method uses String for contractId.
        List<Activity> existingActivitiesForPrestationInContract = activityRepository.findByContractIdAndSalesSystemId(currentContract.id, activityInput.salesSystemId);
        
        double totalUnitsConsumedSoFar = existingActivitiesForPrestationInContract.stream()
            .mapToDouble(a -> a.unitsConsumed)
            .sum();

        if (totalUnitsConsumedSoFar + activityInput.unitsConsumed > soldPrestationInContract.units) {
            throw new IllegalArgumentException("Total units consumed (" + (totalUnitsConsumedSoFar + activityInput.unitsConsumed) + 
                                               ") would exceed contracted units (" + soldPrestationInContract.units + 
                                               ") for prestation " + activityInput.salesSystemId + " in contract " + currentContract.id);
        }

        Prestation prestation = prestationRepository.findById(activityInput.salesSystemId)
            .orElseThrow(() -> new IllegalArgumentException("Prestation details not found for salesSystemId: " + activityInput.salesSystemId));

        Activity newActivity = new Activity();
        newActivity.customerId = customerOid;
        newActivity.contractId = currentContract.id; // Activity.contractId is String now
        newActivity.salesSystemId = activityInput.salesSystemId;
        newActivity.name = prestation.name; 
        newActivity.doneOn = activityInput.doneOn;
        newActivity.unitsConsumed = activityInput.unitsConsumed;

        return activityRepository.save(newActivity);
    }

    public List<Activity> getActivitiesForContract(String contractId) {
        if (contractId == null || contractId.trim().isEmpty()) {
            throw new IllegalArgumentException("Contract ID cannot be null or empty.");
        }
        // Optional: Check if the contract actually exists via CustomerRepository 
        // to return a more specific "contract not found" or just return an empty list if no activities.
        // For now, just query activities.
        return activityRepository.findByContractId(contractId);
    }
}
