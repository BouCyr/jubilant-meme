package klee.solution.bulille.pocs.blink.appserver.in.http;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ActivityInput;
import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.process.ActivityProcessing; // Modified import
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;


@RestController
@RequestMapping("/activities")
public class ActivityController {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ActivityController.class);

    private final ActivityProcessing activityProcess; // Changed type to ActivityProcessing

    public ActivityController(ActivityProcessing activityProcess) { // Changed constructor parameter type
        this.activityProcess = activityProcess;
    }

    @PostMapping
    public ResponseEntity<?> addActivity(@RequestBody ActivityInput activityInput) {
        LOGGER.info("addActivity called with customerId: {}", activityInput.customerId());
        try {
            // Basic validation of input DTO (can be enhanced with @Valid)
            if (activityInput == null || activityInput.customerId() == null || activityInput.contractId() == null ||
                activityInput.salesSystemId() == null || activityInput.doneOn() == null) {
                LOGGER.warn("addActivity failed due to missing required fields in input: {}", activityInput);
                return ResponseEntity.badRequest().body("Missing required fields in activity input.");
            }
            CustomerId customerId = new CustomerId(activityInput.customerId());
            ContractId contractId = new ContractId(activityInput.contractId());
            SalesSystemId salesSystemId = new SalesSystemId(activityInput.salesSystemId());
            Activity createdActivity = this.activityProcess.addActivity(customerId, contractId, salesSystemId, activityInput.doneOn(), activityInput.unitsConsumed());
            LOGGER.info("addActivity completed successfully for customerId: {}", activityInput.customerId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdActivity);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("addActivity failed due to bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage()); // Return message for client clarity
        } catch (Exception e) {
            LOGGER.error("Unexpected error in addActivity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<?> getActivitiesForContract(@PathVariable String contractId) {
        LOGGER.info("getActivitiesForContract called with contractId: {}", contractId);
        try {
            ContractId typedContractId = new ContractId(contractId);
            List<Activity> activities = this.activityProcess.getActivitiesForContract(typedContractId);
            LOGGER.info("getActivitiesForContract returning {} activities for contractId: {}", activities.size(), contractId);
            // No activities found is not an error, just an empty list.
            return ResponseEntity.ok(activities);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("getActivitiesForContract failed due to bad request for contractId {}: {}", contractId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in getActivitiesForContract for contractId {}: {}", contractId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}
