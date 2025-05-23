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

import java.io.File;
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

    private static final Logger logger = LoggerFactory.getLogger(PrestationCsvReaderService.class);
    private final PrestationRepository prestationRepository;
    private final Path inputPath;
    private final Path archivePath;
    private static final String CSV_FILENAME = "prestations.csv";

    public PrestationCsvReaderService(PrestationRepository prestationRepository, FileStorageProperties fileStorageProperties) {
        this.prestationRepository = prestationRepository;
        // Ensure inputFolderPath is not null or empty before creating Path objects
        String inputFolder = fileStorageProperties.getInputFolderPath();
        if (inputFolder == null || inputFolder.trim().isEmpty()) {
            logger.error("Input folder path is not configured!");
            // This is a critical configuration error. Application might not function as expected.
            // Consider throwing an exception or specific handling. For now, paths might be invalid.
            this.inputPath = Paths.get("./input_default_error"); // Fallback, likely problematic
            this.archivePath = Paths.get("./input_default_error/archive");
        } else {
            this.inputPath = Paths.get(inputFolder).toAbsolutePath();
            this.archivePath = Paths.get(inputFolder, "archive").toAbsolutePath();
        }

        try {
            Files.createDirectories(this.inputPath);
            Files.createDirectories(this.archivePath);
            logger.info("Input directory: {}", this.inputPath.toString());
            logger.info("Archive directory: {}", this.archivePath.toString());
        } catch (IOException e) {
            logger.error("Could not create input or archive directory!", e);
            // This could be a fatal error for this service's functionality.
        }
    }

    // Schedule to run, e.g., every minute. Adjust cron as needed.
    // For testing, every minute is fine. For production, it might be less frequent.
    @Scheduled(cron = "0 * * * * ?") // Every minute
    public void processPrestationCsvFile() {
        Path csvFile = inputPath.resolve(CSV_FILENAME);
        if (Files.exists(csvFile) && Files.isRegularFile(csvFile)) {
            logger.info("Found {} file. Processing...", CSV_FILENAME);
            try (FileReader fileReader = new FileReader(csvFile.toFile());
                 CSVReader csvReader = new CSVReader(fileReader)) {

                String[] header = csvReader.readNext(); // Read header
                if (header == null || header.length < 3) { // Basic validation for expected columns
                    logger.error("CSV header is missing or does not have enough columns. Expected salesSystemId,name,unitPrice");
                    // Potentially move to a "failed" directory or log extensively
                    return;
                }
                // Simple validation of header names (optional but good)
                if (!"salesSystemId".equalsIgnoreCase(header[0]) ||
                    !"name".equalsIgnoreCase(header[1]) ||
                    !"unitPrice".equalsIgnoreCase(header[2])) {
                    logger.warn("CSV header names do not match expected 'salesSystemId,name,unitPrice'. Processing based on column order.");
                }


                String[] line;
                while ((line = csvReader.readNext()) != null) {
                    if (line.length >= 3) {
                        String salesSystemId = line[0];
                        String name = line[1];
                        double unitPrice;
                        try {
                            unitPrice = Double.parseDouble(line[2]);
                        } catch (NumberFormatException e) {
                            logger.error("Skipping line due to invalid unitPrice format: {}. Line: {}", line[2], String.join(",", line));
                            continue;
                        }

                        Optional<Prestation> existingPrestationOpt = prestationRepository.findById(salesSystemId);
                        if (existingPrestationOpt.isPresent()) {
                            Prestation prestationToUpdate = existingPrestationOpt.get();
                            prestationToUpdate.name = name;
                            prestationToUpdate.unitPrice = unitPrice;
                            prestationRepository.save(prestationToUpdate);
                            logger.info("Updated prestation: {}", salesSystemId);
                        } else {
                            Prestation newPrestation = new Prestation();
                            newPrestation.salesSystemId = salesSystemId;
                            newPrestation.name = name;
                            newPrestation.unitPrice = unitPrice;
                            prestationRepository.save(newPrestation);
                            logger.info("Created new prestation: {}", salesSystemId);
                        }
                    } else {
                         logger.warn("Skipping malformed line (not enough columns): {}", String.join(",", line));
                    }
                }
                // Move processed file to archive
                archiveFile(csvFile);

            } catch (IOException | CsvValidationException e) {
                logger.error("Error processing CSV file {}: ", CSV_FILENAME, e);
                // Consider moving to a "failed" directory
            }
        } else {
            // logger.trace("No {} file found in {}.", CSV_FILENAME, inputPath); // Can be too verbose
        }
    }

    private void archiveFile(Path fileToArchive) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            Path archivedFileName = Paths.get(CSV_FILENAME + "." + timestamp);
            Path targetPath = archivePath.resolve(archivedFileName);
            Files.move(fileToArchive, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Archived processed file to: {}", targetPath.toString());
        } catch (IOException e) {
            logger.error("Could not archive file {}: ", fileToArchive.toString(), e);
        }
    }
}
