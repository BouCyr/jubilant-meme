package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Prestation {
    @Id
    public String salesSystemId;

    public String name;
    public double unitPrice;
}
