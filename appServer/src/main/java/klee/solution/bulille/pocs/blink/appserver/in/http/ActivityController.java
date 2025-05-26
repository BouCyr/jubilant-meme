package klee.solution.bulille.pocs.blink.appserver.in.http;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs.ActivityInput;
import klee.solution.bulille.pocs.blink.appserver.middle.ActivityService;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;

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

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<?> addActivity(@RequestBody ActivityInput activityInput) {
        try {
            // Basic validation of input DTO (can be enhanced with @Valid)
            if (activityInput == null || activityInput.customerId == null || activityInput.contractId == null || 
                activityInput.salesSystemId == null || activityInput.doneOn == null) {
                return ResponseEntity.badRequest().body("Missing required fields in activity input.");
            }
            Activity createdActivity = activityService.addActivity(activityInput);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdActivity);
        } catch (IllegalArgumentException e) {
            // Log e.getMessage() for server-side tracking
            return ResponseEntity.badRequest().body(e.getMessage()); // Return message for client clarity
        } catch (Exception e) {
            // Log e for server-side tracking
            // In a real app, return a generic error message for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<?> getActivitiesForContract(@PathVariable String contractId) {
        try {
            List<Activity> activities = activityService.getActivitiesForContract(contractId);
            // No activities found is not an error, just an empty list.
            return ResponseEntity.ok(activities);
        } catch (IllegalArgumentException e) {
            // Log e.getMessage()
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Log e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}
