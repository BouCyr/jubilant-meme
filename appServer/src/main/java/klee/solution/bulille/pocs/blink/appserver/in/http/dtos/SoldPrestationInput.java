package klee.solution.bulille.pocs.blink.appserver.in.http.dtos;

// Add validation annotations if desired (e.g., @NotBlank, @Min(1))
public class SoldPrestationInput {
    public String salesSystemId; // ID of the Prestation
    public long units;           // Number of units sold
    public double totalBilledAmountForUnits; // Total price for these units for the contract duration
}
