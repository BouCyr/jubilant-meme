package klee.solution.bulille.pocs.blink.appserver.in.http.dtos;

import java.time.LocalDate;
// Add any validation annotations if desired (e.g., @NotBlank, @NotNull)
// For now, keeping it simple. Consider adding javax.validation or jakarta.validation dependency if needed.

public class CustomerInput {

    public String firstName;
    public String givenName;
    public LocalDate dateOfBirth;

    // Getters and setters can be added if needed by frameworks, 
    // or if you prefer private fields, but public fields are fine for simple DTOs.
}
