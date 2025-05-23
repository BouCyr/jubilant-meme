package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document("activity")
public class Activity {

    @Id
    public ObjectId id;

    public String name;
    public String salesSystemId;

    public LocalDate doneOn;
}
