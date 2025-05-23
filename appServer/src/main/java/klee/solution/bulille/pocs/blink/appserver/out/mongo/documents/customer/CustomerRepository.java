package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, ObjectId> {
}
