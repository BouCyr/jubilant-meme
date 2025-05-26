package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs;

import java.time.LocalDate;
// Add validation annotations as needed (e.g., @NotNull, @FutureOrPresent for doneOn)

public class ActivityInput {
    public String customerId;       // String representation of ObjectId
    public String contractId;       // String representation of Contract's internal ID
    public String salesSystemId;    // ID of the Prestation
    public LocalDate doneOn;
    public double unitsConsumed;
    // 'name' field for Activity can be derived from Prestation or set if needed.
    // For now, Activity.name will be populated from Prestation.name.
}
