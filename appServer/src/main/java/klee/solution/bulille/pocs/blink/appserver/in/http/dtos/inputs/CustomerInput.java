package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs;

import java.time.LocalDate;

public record CustomerInput(
    String firstName,
    String givenName,
    LocalDate dateOfBirth
) {
    // No explicit getters, setters, or constructor needed for basic records.
    // Compact constructor can be added for validation if needed.
}
