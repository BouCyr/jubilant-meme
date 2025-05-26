package klee.solution.bulille.pocs.blink.appserver.middle.process;

import klee.solution.bulille.pocs.blink.appserver.middle.ActivityServicing;
import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;

@Component
class ActivityProcess implements ActivityProcessing {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ActivityProcess.class);

    private final ActivityServicing activityService;

    public ActivityProcess(ActivityServicing activityService) {
        this.activityService = activityService;
    }

    @Override
    @Transactional
    public Activity addActivity(@NonNull CustomerId customerId, @NonNull ContractId contractId, @NonNull SalesSystemId salesSystemId, @NonNull LocalDate doneOn, double unitsConsumed) {
        LOGGER.info("addActivity called in ActivityProcess for customerId: {}, contractId: {}, salesSystemId: {}", customerId.id(), contractId.value(), salesSystemId.value());
        Activity activity = this.activityService.addActivity(customerId, contractId, salesSystemId, doneOn, unitsConsumed);
        LOGGER.info("addActivity successfully completed in ActivityProcess for customerId: {}, contractId: {}, salesSystemId: {}", customerId.id(), contractId.value(), salesSystemId.value());
        return activity;
    }

    @Override
    public List<Activity> getActivitiesForContract(@NonNull ContractId contractId) {
        LOGGER.info("getActivitiesForContract called in ActivityProcess for contractId: {}", contractId.value());
        List<Activity> activities = this.activityService.getActivitiesForContract(contractId);
        LOGGER.info("getActivitiesForContract successfully completed in ActivityProcess for contractId: {}. Found {} activities.", contractId.value(), activities.size());
        return activities;
    }
}
