package klee.solution.bulille.pocs.blink.appserver.middle.id;

public record ContractId(String id) {
    public ContractId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ContractId ID cannot be null or empty.");
        }
    }

    public String value() {
        return id;
    }
}
