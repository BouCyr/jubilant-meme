package klee.solution.bulille.pocs.blink.appserver.middle.services.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.ActivityFinderByContract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.ActivityFinderByContractAndPresta;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.ActivityWriter;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerRepository;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.SoldPrestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;

import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;

@Service
class ActivityService implements ActivityCreator, ActivityLister {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ActivityService.class);

    private final CustomerRepository customerRepository;
    private final PrestationRepository prestationRepository; 
    private final ActivityFinderByContractAndPresta findByContractIdAndPrestation;
    private final ActivityFinderByContract findByContractId;
    private final ActivityWriter activityWriter;

    public ActivityService(

            CustomerRepository customerRepository,
            PrestationRepository prestationRepository, ActivityFinderByContractAndPresta findByContractIdAndPrestation, ActivityFinderByContract findByContractId, ActivityWriter activityWriter) {
        this.customerRepository = customerRepository;
        this.prestationRepository = prestationRepository;
        this.findByContractIdAndPrestation = findByContractIdAndPrestation;
        this.findByContractId = findByContractId;
        this.activityWriter = activityWriter;
    }

    @Override
    public Activity create(@NonNull CustomerId customerId, @NonNull ContractId contractId, @NonNull SalesSystemId salesSystemId, @NonNull LocalDate doneOn, double unitsConsumed) {
        LOGGER.info("addActivity called for customerId: {}, contractId: {}, salesSystemId: {}", customerId.id(), contractId.value(), salesSystemId.value());
        try {
            if (unitsConsumed <= 0) {
                LOGGER.warn("addActivity failed: Units consumed must be positive. Received: {}", unitsConsumed);
                throw new IllegalArgumentException("Units consumed must be positive.");
            }
            // doneOn is @NonNull, so no null check needed here by contract
            if (doneOn.isAfter(LocalDate.now())) {
                LOGGER.warn("addActivity failed: Activity date cannot be in the future. Received: {}", doneOn);
                throw new IllegalArgumentException("Activity date cannot be in the future.");
            }

            LOGGER.info("Fetching customer with ID: {}", customerId.id());
            Customer customer = this.customerRepository.findById(customerId.toOID())
                .orElseThrow(() -> {
                    LOGGER.warn("addActivity failed: Customer not found with ID: {}", customerId.id());
                    return new IllegalArgumentException("Customer not found with ID: " + customerId.id());
                });

            Contract currentContract = customer.contracts.stream()
                .filter(c -> c.id.equals(contractId.value()))
                .findFirst()
                .orElseThrow(() -> {
                    LOGGER.warn("addActivity failed: Contract not found with ID: {} for customer {}", contractId.value(), customerId.id());
                    return new IllegalArgumentException("Contract not found with ID: " + contractId.value() + " for customer " + customerId.id());
                });

            if (currentContract.start.isAfter(doneOn) ||
                (currentContract.end != null && currentContract.end.isBefore(doneOn))) {
                LOGGER.warn("addActivity failed: Activity date {} is not within the contract period ({} - {}).", doneOn, currentContract.start, (currentContract.end == null ? "Permanent" : currentContract.end));
                throw new IllegalArgumentException("Activity date " + doneOn + " is not within the contract period (" + currentContract.start + " - " + (currentContract.end == null ? "Permanent" : currentContract.end) + ").");
            }

            SoldPrestation soldPrestationInContract = currentContract.soldPrestations.stream()
                .filter(sp -> sp.salesSystemId.equals(salesSystemId.value()))
                .findFirst()
                .orElseThrow(() -> {
                    LOGGER.warn("addActivity failed: Prestation with salesSystemId: {} not found in contract {}", salesSystemId.value(), contractId.value());
                    return new IllegalArgumentException("Prestation with salesSystemId: " + salesSystemId.value() + " not found in contract " + contractId.value());
                });

            LOGGER.info("Fetching existing activities for contractId: {} and salesSystemId: {}", contractId.value(), salesSystemId.value());
            List<Activity> existingActivitiesForPrestationInContract = this.findByContractIdAndPrestation.byContractAndItem(
                    contractId,
                    salesSystemId);

            double totalUnitsConsumedSoFar = existingActivitiesForPrestationInContract.stream()
                .mapToDouble(a -> a.unitsConsumed)
                .sum();

            if (totalUnitsConsumedSoFar + unitsConsumed > soldPrestationInContract.units) {
                LOGGER.warn("addActivity failed: Total units consumed ({}) would exceed contracted units ({}) for prestation {} in contract {}.",
                            (totalUnitsConsumedSoFar + unitsConsumed), soldPrestationInContract.units, salesSystemId.value(), contractId.value());
                throw new IllegalArgumentException("Total units consumed (" + (totalUnitsConsumedSoFar + unitsConsumed) +
                                                   ") would exceed contracted units (" + soldPrestationInContract.units +
                                                   ") for prestation " + salesSystemId.value() + " in contract " + contractId.value());
            }

            LOGGER.info("Fetching prestation details for salesSystemId: {}", salesSystemId.value());
            Prestation prestation = this.prestationRepository.findById(salesSystemId.value())
                .orElseThrow(() -> {
                    LOGGER.warn("addActivity failed: Prestation details not found for salesSystemId: {}", salesSystemId.value());
                    return new IllegalArgumentException("Prestation details not found for salesSystemId: " + salesSystemId.value());
                });

            Activity newActivity = new Activity();
            newActivity.customerId = customerId.toOID();
            newActivity.contractId = contractId.value();
            newActivity.salesSystemId = salesSystemId.value();
            newActivity.name = prestation.name;
            newActivity.doneOn = doneOn;
            newActivity.unitsConsumed = unitsConsumed;

            LOGGER.info("Attempting to save new activity for customerId: {}", customerId.id());
            Activity savedActivity = this.activityWriter.save(newActivity);
            LOGGER.info("Successfully saved new activity with ID: {} for customerId: {}", savedActivity.id, customerId.id());
            LOGGER.info("addActivity completed successfully. Activity ID: {}", savedActivity.id);
            return savedActivity;
        } catch (IllegalArgumentException e) {
            // Already logged with WARN, rethrow for controller to handle
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in addActivity for customerId: {}", customerId.id(), e);
            throw new RuntimeException("An unexpected error occurred while adding activity.", e); // Or a custom domain exception
        }
    }

    @Override
    public List<Activity> list(@NonNull ContractId contractId) {
        LOGGER.info("getActivitiesForContract called for contractId: {}", contractId.value());
        // contractId is @NonNull, so no null check needed by contract. The existing check for trim().isEmpty() is still valid.
        if (contractId.value().trim().isEmpty()) {
            LOGGER.warn("getActivitiesForContract failed: Contract ID value cannot be empty.");
            throw new IllegalArgumentException("Contract ID value cannot be empty.");
        }
        try {
            LOGGER.info("Fetching activities for contractId: {}", contractId.value());
            List<Activity> activities = this.findByContractId.byContract(contractId);
            LOGGER.info("getActivitiesForContract completed successfully. Found {} activities for contractId: {}", activities.size(), contractId.value());
            return activities;
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getActivitiesForContract for contractId: {}", contractId.value(), e);
            throw new RuntimeException("An unexpected error occurred while fetching activities for contract.", e);
        }
    }
}
