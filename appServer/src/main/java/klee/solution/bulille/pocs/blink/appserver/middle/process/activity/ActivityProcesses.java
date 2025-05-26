package klee.solution.bulille.pocs.blink.appserver.middle.process.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.services.activity.ActivityCreator;
import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;
import klee.solution.bulille.pocs.blink.appserver.middle.services.activity.ActivityLister;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;

@Component
class ActivityProcesses implements ActivityListing, ActivityCreation {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ActivityProcesses.class);

    private final ActivityCreator activityService;
    private final ActivityLister activityLister;

    public ActivityProcesses(ActivityCreator activityService, ActivityLister activityLister) {
        this.activityService = activityService;
        this.activityLister = activityLister;
    }

    @Override
    @Transactional
    public Activity create(@NonNull CustomerId customerId, @NonNull ContractId contractId, @NonNull SalesSystemId salesSystemId, @NonNull LocalDate doneOn, double unitsConsumed) {
        LOGGER.info("addActivity called in ActivityProcesses for customerId: {}, contractId: {}, salesSystemId: {}", customerId.id(), contractId.value(), salesSystemId.value());
        Activity activity = this.activityService.create(customerId, contractId, salesSystemId, doneOn, unitsConsumed);
        LOGGER.info("addActivity successfully completed in ActivityProcesses for customerId: {}, contractId: {}, salesSystemId: {}", customerId.id(), contractId.value(), salesSystemId.value());
        return activity;
    }

    @Override
    public List<Activity> find(@NonNull ContractId contractId) {
        LOGGER.info("getActivitiesForContract called in ActivityProcesses for contractId: {}", contractId.value());
        List<Activity> activities = this.activityLister.list(contractId);
        LOGGER.info("getActivitiesForContract successfully completed in ActivityProcesses for contractId: {}. Found {} activities.", contractId.value(), activities.size());
        return activities;
    }
}
