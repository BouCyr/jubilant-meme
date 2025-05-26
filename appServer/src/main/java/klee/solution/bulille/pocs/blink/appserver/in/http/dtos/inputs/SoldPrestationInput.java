package klee.solution.bulille.pocs.blink.appserver.in.http.dtos.inputs;

// Add validation annotations if desired (e.g., @NotBlank, @Min(1))
public record SoldPrestationInput(
    String salesSystemId, // ID of the Prestation
    long units,           // Number of units sold
    double totalBilledAmountForUnits // Total price for these units for the contract duration
) {
}
