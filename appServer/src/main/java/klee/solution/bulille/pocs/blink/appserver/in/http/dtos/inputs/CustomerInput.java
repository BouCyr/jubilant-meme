package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs;

import java.time.LocalDate;
// Add any validation annotations if desired (e.g., @NotBlank, @NotNull)
// For now, keeping it simple. Consider adding javax.validation or jakarta.validation dependency if needed.

public record CustomerInput(
    String firstName,
    String givenName,
    LocalDate dateOfBirth
    // Getters and setters comments are no longer relevant for records.
) {
}
