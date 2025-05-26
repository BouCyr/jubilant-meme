package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestationRepository extends MongoRepository<Prestation, String> {
    // String is the type of Prestation.salesSystemId
}
