package klee.solution.bulille.pocs.blink.appserver.middle.id;

import org.bson.types.ObjectId;
// Or any other way to check for empty string

public record CustomerId(String id) {
    public CustomerId {
        if (id == null || id.isBlank()) { // Or id == null || id.isBlank() for pure Java
            throw new IllegalArgumentException("CustomerId ID cannot be null or empty.");
        }
    }

    public ObjectId toOID() {
        return new ObjectId(id);
    }

    // value() method is not strictly needed due to record's id() accessor
    // public String value() { return id; }
}
