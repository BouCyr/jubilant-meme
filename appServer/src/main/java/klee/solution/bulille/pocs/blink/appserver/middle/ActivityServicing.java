package klee.solution.bulille.pocs.blink.appserver.middle;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import org.springframework.lang.NonNull;
import java.time.LocalDate;
import java.util.List;

public interface ActivityServicing {
    Activity addActivity(@NonNull CustomerId customerId, @NonNull ContractId contractId, @NonNull SalesSystemId salesSystemId, @NonNull LocalDate doneOn, double unitsConsumed);
    List<Activity> getActivitiesForContract(@NonNull ContractId contractId);
}
