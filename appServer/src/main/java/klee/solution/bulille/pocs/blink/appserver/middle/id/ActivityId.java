package klee.solution.bulille.pocs.blink.appserver.middle.id;

import org.bson.types.ObjectId;

public record ActivityId(ObjectId id) {
    public ActivityId {
        if (id == null) {
            throw new IllegalArgumentException("ActivityId ID cannot be null.");
        }
    }

    public ObjectId value() {
        return id;
    }
}
