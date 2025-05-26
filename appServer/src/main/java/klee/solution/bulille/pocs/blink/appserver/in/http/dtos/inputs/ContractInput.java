package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs;

import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.ContractType;
import java.time.LocalDate;
import java.util.List;
// Add validation annotations if desired

public record ContractInput(
    ContractType type,
    LocalDate startDate,
    LocalDate endDate, // Optional, especially for PERMANENT type
    List<SoldPrestationInput> soldPrestations
) {
}
