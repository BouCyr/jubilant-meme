package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

import klee.solution.bulille.pocs.blink.appserver.middle.id.ContractId;

import java.util.List;

@FunctionalInterface
public interface ActivityFinderByContract {
    List<Activity> byContract(ContractId contractId);
}
