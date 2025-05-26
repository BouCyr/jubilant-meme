package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;
import klee.solution.bulille.pocs.blink.appserver.middle.id.SalesSystemId;

import java.util.List;

@FunctionalInterface
public interface ActivityFinderByContractAndPresta {
    List<Activity> byContractAndItem(ContractId contractId, SalesSystemId salesSystemId);
}
