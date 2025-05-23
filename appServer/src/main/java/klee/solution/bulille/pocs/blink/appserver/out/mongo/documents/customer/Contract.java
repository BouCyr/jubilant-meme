package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;

import java.time.LocalDate;
import java.util.List;

public class Contract {

    public ContractType type;

    public LocalDate start;
    public LocalDate end;

    public List<Prestation> prestations;
}
