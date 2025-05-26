package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Contract {

    public String id = UUID.randomUUID().toString();
    public ContractType type;

    public LocalDate start;
    public LocalDate end;

    public List<SoldPrestation> soldPrestations;
}
