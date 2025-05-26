package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs;

import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;

import java.time.LocalDate;
import java.util.List;

public record CustomerOutput(String id,
                             String firstName,
                             String givenName,
                             LocalDate dateOfBirth,
                             List<Contract> contracts) {

    public static CustomerOutput from(Customer customer) {
        return new CustomerOutput(
                customer.id.toString(),
                customer.firstName,
                customer.givenName,
                customer.dateOfBirth,
                customer.contracts
        );
    }
}
