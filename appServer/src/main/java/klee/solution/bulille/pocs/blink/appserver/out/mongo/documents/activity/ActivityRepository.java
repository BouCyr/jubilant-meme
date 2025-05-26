package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Required for potential future methods

@Repository
public interface ActivityRepository extends MongoRepository<Activity, ObjectId> {
    // ObjectId is the type of Activity.id
    List<Activity> findByContractIdAndSalesSystemId(String contractId, String salesSystemId);
    List<Activity> findByContractId(String contractId); // Ensure this method is present
}
