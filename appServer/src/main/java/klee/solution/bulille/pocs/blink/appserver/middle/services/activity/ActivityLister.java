package klee.solution.bulille.pocs.blink.appserver.middle.services.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import org.springframework.lang.NonNull;

import java.util.List;

@FunctionalInterface
public interface ActivityLister {
    List<Activity> list(@NonNull ContractId contractId);

}
