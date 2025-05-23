package klee.solution.bulille.pocs.blink.appserver.middle.id;

import org.bson.types.ObjectId;

public record CustomerId(String id) {


    public ObjectId toOID(){
        return new ObjectId(id);
    }
}
