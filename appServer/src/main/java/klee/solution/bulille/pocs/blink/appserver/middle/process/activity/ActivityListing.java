package klee.solution.bulille.pocs.blink.appserver.middle.process.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import org.springframework.lang.NonNull;

import java.util.List;

@FunctionalInterface
public interface ActivityListing {
    List<Activity> find(@NonNull ContractId contractId);
}
