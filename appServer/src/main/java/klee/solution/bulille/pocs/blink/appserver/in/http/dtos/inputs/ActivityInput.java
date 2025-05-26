package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs;

import java.time.LocalDate;
// Add validation annotations as needed (e.g., @NotNull, @FutureOrPresent for doneOn)

public record ActivityInput(
    String customerId,       // String representation of ObjectId
    String contractId,       // String representation of Contract's internal ID
    String salesSystemId,    // ID of the Prestation
    LocalDate doneOn,
    double unitsConsumed
    // 'name' field for Activity can be derived from Prestation or set if needed.
    // For now, Activity.name will be populated from Prestation.name.
) {
}
