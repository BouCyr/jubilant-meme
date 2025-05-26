package klee.solution.bulille.pocs.blink.appserver.in.file;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import klee.solution.bulille.pocs.blink.appserver.config.FileStorageProperties;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.Prestation;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.prestation.PrestationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PrestationCsvReaderService {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PrestationCsvReaderService.class);

    private final PrestationRepository prestationRepository;
    private final Path inputPath;
    private final Path archivePath;
    private static final String CSV_FILENAME = "prestations.csv";

    public PrestationCsvReaderService(PrestationRepository prestationRepository, FileStorageProperties fileStorageProperties) {
        this.prestationRepository = prestationRepository;

        this.inputPath = fileStorageProperties.input();
        this.archivePath = fileStorageProperties.archives();

        try {
            Files.createDirectories(this.inputPath);
            Files.createDirectories(this.archivePath);
            LOGGER.info("Input directory: {}", this.inputPath);
            LOGGER.info("Archive directory: {}", this.archivePath);
        } catch (IOException e) {
            LOGGER.error("Could not create input or archive directory!", e);
            // This could be a fatal error for this service's functionality.
        }
    }

    // Schedule to run, e.g., every minute. Adjust cron as needed.
    // For testing, every minute is fine. For production, it might be less frequent.
    @Scheduled(cron = "0 * * * * ?") // Every minute
    public void processPrestationCsvFile() {
        LOGGER.info("processPrestationCsvFile started.");
        Path csvFile = this.inputPath.resolve(CSV_FILENAME);
        if (Files.exists(csvFile) && Files.isRegularFile(csvFile)) {
            LOGGER.info("Found {} file. Processing...", CSV_FILENAME);
            try (FileReader fileReader = new FileReader(csvFile.toFile());
                 CSVReader csvReader = new CSVReader(fileReader)) {

                String[] header = csvReader.readNext(); // Read header
                if (header == null || header.length < 3) { // Basic validation for expected columns
                    LOGGER.error("CSV header is missing or does not have enough columns. Expected salesSystemId,name,unitPrice. File: {}", csvFile);
                    // Potentially move to a "failed" directory or log extensively
                    return;
                }
                // Simple validation of header names (optional but good)
                if (!"salesSystemId".equalsIgnoreCase(header[0]) ||
                    !"name".equalsIgnoreCase(header[1]) ||
                    !"unitPrice".equalsIgnoreCase(header[2])) {
                    LOGGER.warn("CSV header names do not match expected 'salesSystemId,name,unitPrice'. Processing based on column order. File: {}", csvFile);
                }

                String[] line;
                int lineCount = 0;
                int processedCount = 0;
                while ((line = csvReader.readNext()) != null) {
                    lineCount++;
                    if (line.length >= 3) {
                        String salesSystemId = line[0];
                        String name = line[1];
                        double unitPrice;
                        try {
                            unitPrice = Double.parseDouble(line[2]);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Skipping line {} due to invalid unitPrice format: '{}'. Line content: '{}'. File: {}", lineCount, line[2], String.join(",", line), csvFile);
                            continue;
                        }

                        Optional<Prestation> existingPrestationOpt = this.prestationRepository.findById(salesSystemId);
                        if (existingPrestationOpt.isPresent()) {
                            Prestation prestationToUpdate = existingPrestationOpt.get();
                            prestationToUpdate.name = name;
                            prestationToUpdate.unitPrice = unitPrice;
                            LOGGER.info("Attempting to update prestation with salesSystemId: {}", salesSystemId);
                            Prestation savedPrestation = this.prestationRepository.save(prestationToUpdate);
                            LOGGER.info("Successfully updated prestation with salesSystemId: {}", savedPrestation.salesSystemId);
                            LOGGER.info("Updated prestation: {}. File: {}", salesSystemId, csvFile);
                        } else {
                            Prestation newPrestation = new Prestation();
                            newPrestation.salesSystemId = salesSystemId;
                            newPrestation.name = name;
                            newPrestation.unitPrice = unitPrice;
                            LOGGER.info("Attempting to create new prestation with salesSystemId: {}", salesSystemId);
                            Prestation savedPrestation = this.prestationRepository.save(newPrestation);
                            LOGGER.info("Successfully created new prestation with salesSystemId: {}", savedPrestation.salesSystemId);
                            LOGGER.info("Created new prestation: {}. File: {}", salesSystemId, csvFile);
                        }
                        processedCount++;
                    } else {
                         LOGGER.warn("Skipping malformed line {} (not enough columns): '{}'. File: {}", lineCount, String.join(",", line), csvFile);
                    }
                }
                LOGGER.info("processPrestationCsvFile completed processing of {}. Total lines: {}, Processed lines: {}", CSV_FILENAME, lineCount, processedCount);

            } catch (IOException | CsvValidationException e) {
                LOGGER.error("Error processing CSV file {}: {}", CSV_FILENAME, e.getMessage(), e);
                // Consider moving to a "failed" directory
            }finally {
                // Move processed file to archive
                this.archiveFile(csvFile);
            }
        } else {
            LOGGER.info("No {} file found to process at this time.", CSV_FILENAME);
        }
        LOGGER.info("processPrestationCsvFile finished.");
    }

    private void archiveFile(Path fileToArchive) {
        LOGGER.info("archiveFile called for file: {}", fileToArchive);
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            Path archivedFileName = Paths.get(CSV_FILENAME + "." + timestamp);
            Path targetPath = this.archivePath.resolve(archivedFileName);
            LOGGER.info("Attempting to archive file {} to {}", fileToArchive, targetPath);
            Files.move(fileToArchive, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Successfully archived file {} to {}", fileToArchive, targetPath);
            LOGGER.info("Archived processed file to: {}", targetPath);
        } catch (IOException e) {
            LOGGER.error("Could not archive file {}: {}", fileToArchive, e.getMessage(), e);
        }
    }
}
