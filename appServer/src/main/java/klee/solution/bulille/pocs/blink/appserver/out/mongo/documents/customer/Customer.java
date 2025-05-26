package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;

@Document("users")
public class Customer {

    @Id
    public ObjectId id;

    public String firstName;
    public String givenName;
    public LocalDate dateOfBirth;

    public List<Contract> contracts = new ArrayList<>();




}
