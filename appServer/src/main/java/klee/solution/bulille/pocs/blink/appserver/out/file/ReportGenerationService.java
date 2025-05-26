package klee.solution.bulille.pocs.blink.appserver.out.file;

import com.opencsv.CSVWriter;
import klee.solution.bulille.pocs.blink.appserver.config.FileStorageProperties;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.Activity;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.activity.ActivityRepository;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.CustomerRepository;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Contract;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import klee.solution.bulille.pocs.blink.appserver.out.file.ReportGeneration; // Added import

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;


@Service
class ReportGenerationService implements ReportGeneration { // Modified class declaration
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ReportGenerationService.class);

    private final CustomerRepository customerRepository;
    private final ActivityRepository activityRepository;
    private final PrestationRepository prestationRepository;
    private final Path outputReportingPath;

    public ReportGenerationService(CustomerRepository customerRepository,
                                   ActivityRepository activityRepository,
                                   PrestationRepository prestationRepository,
                                   FileStorageProperties fileStorageProperties) {
        this.customerRepository = customerRepository;
        this.activityRepository = activityRepository;
        this.prestationRepository = prestationRepository;

        this.outputReportingPath = fileStorageProperties.output();

        try {
            Files.createDirectories(this.outputReportingPath);
            LOGGER.info("Output (report) directory: {}", this.outputReportingPath);
        } catch (IOException e) {
            LOGGER.error("Could not create output (report) directory!", e);
        }
    }

    // Scheduled to run every hour at the top of the hour
    @Scheduled(cron = "0 0 * * * ?")
    @Override // Added @Override
    public void generateHourlyReport() {
        LOGGER.info("generateHourlyReport started.");
        LOGGER.info("Attempting to fetch all customers for report generation.");
        List<Customer> customers = this.customerRepository.findAll();
        LOGGER.info("Successfully fetched {} customers for report generation.", customers.size());
        if (customers.isEmpty()) {
            LOGGER.info("No customers found. Report generation skipped.");
            return;
        }

        // Fetch all necessary prestations into a map for quick lookup
        LOGGER.info("Attempting to fetch all prestations for report generation.");
        Map<String, Prestation> prestationMap = this.prestationRepository.findAll().stream()
            .collect(Collectors.toMap(Prestation::salesSystemId, Function.identity()));
        LOGGER.info("Successfully fetched {} prestations into map for report generation.", prestationMap.size());
        LOGGER.debug("Prestation map created with {} entries.", prestationMap.size());

        List<String[]> reportData = new ArrayList<>();
        reportData.add(new String[]{"contract_id", "sum_billed_activity_amount_euris", "remaining_balance_euris"});

        int contractsProcessed = 0;
        for (Customer customer : customers) {
            LOGGER.debug("Processing customer: {}", customer.getId());
            for (Contract contract : customer.contracts) {
                // Check if contract is ongoing
                if (contract.end == null || contract.end.isAfter(LocalDate.now().minusDays(1))) { // contract.end is inclusive
                    LOGGER.debug("Processing ongoing contract: {}", contract.id);
                    LOGGER.info("Attempting to fetch activities for contractId: {}", contract.id);
                    List<Activity> activitiesForContract = this.activityRepository.findByContractId(contract.id);
                    LOGGER.info("Successfully fetched {} activities for contractId: {}", activitiesForContract.size(), contract.id);
                    
                    double totalActivityBilledAmount = 0.0;
                    for (Activity activity : activitiesForContract) {
                        Prestation prestation = prestationMap.get(activity.salesSystemId);
                        if (prestation != null) {
                            totalActivityBilledAmount += activity.unitsConsumed * prestation.unitPrice;
                        } else {
                            LOGGER.warn("Prestation details not found for salesSystemId: {} during report generation for contract: {}. Activity amount for this will be 0.", activity.salesSystemId, contract.id);
                        }
                    }

                    double contractTotalValue = contract.soldPrestations.stream()
                        .mapToDouble(sp -> sp.totalBilledAmountForUnits)
                        .sum();
                    
                    double remainingBalance = contractTotalValue - totalActivityBilledAmount;

                    reportData.add(new String[]{
                        contract.id,
                        String.format("%.2f", totalActivityBilledAmount),
                        String.format("%.2f", remainingBalance)
                    });
                    contractsProcessed++;
                } else {
                    LOGGER.debug("Skipping non-ongoing contract: {}", contract.id);
                }
            }
        }
        LOGGER.info("Processed {} ongoing contracts for report.", contractsProcessed);

        if (reportData.size() <= 1) { // Only header
            LOGGER.info("No ongoing contracts found or no data to report. Report generation skipped.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "report_" + timestamp + ".csv";
        Path reportFilePath = this.outputReportingPath.resolve(fileName);

        try (FileWriter writer = new FileWriter(reportFilePath.toFile());
             CSVWriter csvWriter = new CSVWriter(writer)) {
            LOGGER.info("Attempting to write report to file: {}", reportFilePath);
            csvWriter.writeAll(reportData);
            LOGGER.info("Successfully wrote report to file: {}", reportFilePath);
            LOGGER.info("Successfully generated report: {}", reportFilePath);
        } catch (IOException e) {
            LOGGER.error("Error writing report to CSV file {}: {}", reportFilePath, e.getMessage(), e);
        }
    }
}
