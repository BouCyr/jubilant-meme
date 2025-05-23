package klee.solution.bulille.pocs.blink.appserver.in.http.dtos;

import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.ContractType;
import java.time.LocalDate;
import java.util.List;
// Add validation annotations if desired

public class ContractInput {
    public ContractType type;
    public LocalDate startDate;
    public LocalDate endDate; // Optional, especially for PERMANENT type
    public List<SoldPrestationInput> soldPrestations;
}
