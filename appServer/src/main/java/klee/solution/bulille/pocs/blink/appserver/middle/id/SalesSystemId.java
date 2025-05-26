package klee.solution.bulille.pocs.blink.appserver.middle.id;

public record SalesSystemId(String id) {
    public SalesSystemId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("SalesSystemId ID cannot be null or empty.");
        }
    }

    public String value() {
        return id;
    }
}
