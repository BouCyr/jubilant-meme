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
public class ReportGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);
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
            logger.info("Output (report) directory: {}", this.outputReportingPath);
        } catch (IOException e) {
            logger.error("Could not create output (report) directory!", e);
        }
    }

    // Scheduled to run every hour at the top of the hour
    @Scheduled(cron = "0 0 * * * ?")
    public void generateHourlyReport() {
        logger.info("Starting hourly report generation...");
        List<Customer> customers = customerRepository.findAll();
        if (customers.isEmpty()) {
            logger.info("No customers found. Report generation skipped.");
            return;
        }

        // Fetch all necessary prestations into a map for quick lookup
        Map<String, Prestation> prestationMap = prestationRepository.findAll().stream()
            .collect(Collectors.toMap(p -> p.salesSystemId, Function.identity()));

        List<String[]> reportData = new ArrayList<>();
        reportData.add(new String[]{"contract_id", "sum_billed_activity_amount_euris", "remaining_balance_euris"});

        for (Customer customer : customers) {
            for (Contract contract : customer.contracts) {
                // Check if contract is ongoing
                if (contract.end == null || contract.end.isAfter(LocalDate.now().minusDays(1))) { // contract.end is inclusive
                    List<Activity> activitiesForContract = activityRepository.findByContractId(contract.id);
                    
                    double totalActivityBilledAmount = 0.0;
                    for (Activity activity : activitiesForContract) {
                        Prestation prestation = prestationMap.get(activity.salesSystemId);
                        if (prestation != null) {
                            totalActivityBilledAmount += activity.unitsConsumed * prestation.unitPrice;
                        } else {
                            logger.warn("Prestation details not found for salesSystemId: {} during report generation for contract: {}. Activity amount for this will be 0.", activity.salesSystemId, contract.id);
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
                }
            }
        }

        if (reportData.size() <= 1) { // Only header
            logger.info("No ongoing contracts found or no data to report.");
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = "report_" + timestamp + ".csv";
        Path reportFilePath = outputReportingPath.resolve(fileName);

        try (FileWriter writer = new FileWriter(reportFilePath.toFile());
             CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeAll(reportData);
            logger.info("Successfully generated report: {}", reportFilePath);
        } catch (IOException e) {
            logger.error("Error writing report to CSV file {}: ", reportFilePath, e);
        }
    }
}
