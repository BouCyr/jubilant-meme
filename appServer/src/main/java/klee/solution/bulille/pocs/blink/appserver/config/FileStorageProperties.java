package klee.solution.bulille.pocs.blink.appserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public record FileStorageProperties(@Value("${project.input.folder.path}") Path input,
                                    @Value("${project.archives.folder.path}") Path archives,
                                    @Value("${project.output.folder.path}") Path output) {
}
