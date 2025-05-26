package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

// Ensure Prestation import is correct if needed, though it's not directly used as a field type here.
// import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;

public class SoldPrestation {

    public String salesSystemId; // This ID links to an existing Prestation document
    public long units; // Number of units of this prestation sold in the contract
    public double totalBilledAmountForUnits; // Total amount for these units for the contract duration. (Clarified name from 'billedAmount_euros')

    // Add a constructor if you like, or ensure fields are public for direct access.
    public SoldPrestation() {}

    public SoldPrestation(String salesSystemId, long units, double totalBilledAmountForUnits) {
        this.salesSystemId = salesSystemId;
        this.units = units;
        this.totalBilledAmountForUnits = totalBilledAmountForUnits;
    }
}
