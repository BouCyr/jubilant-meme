package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CustomerRepository extends MongoRepository<Customer, ObjectId> {
    @Query("{$or: [{'firstName': {$regex: ?0, $options: 'i'}}, {'givenName': {$regex: ?0, $options: 'i'}}]}")
    Page<Customer> findByFirstNameOrGivenNameContainingIgnoreCase(String name, Pageable pageable);
}
