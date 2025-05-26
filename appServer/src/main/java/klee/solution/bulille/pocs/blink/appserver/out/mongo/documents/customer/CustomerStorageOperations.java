package klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import java.util.Optional;

public interface CustomerStorageOperations {
    Optional<Customer> find(@NonNull CustomerId customerId);
    Page<Customer> findByFirstNameOrGivenNameContainingIgnoreCase(@NonNull String nameQuery, @NonNull Pageable pageable);
    Page<Customer> findAll(@NonNull Pageable pageable);
    Customer save(@NonNull Customer customer);
}
