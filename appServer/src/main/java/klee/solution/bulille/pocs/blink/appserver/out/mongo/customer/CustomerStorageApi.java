package klee.solution.bulille.pocs.blink.appserver.out.mongo.customer;

import klee.solution.bulille.pocs.blink.appserver.middle.id.CustomerId;
import klee.solution.bulille.pocs.blink.appserver.out.mongo.documents.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface CustomerStorageApi {

    Optional<Customer> find(@NonNull CustomerId customerId);

    // nameQuery can be null or empty
    Page<Customer> findByFirstNameOrGivenNameContainingIgnoreCase(String nameQuery, @NonNull Pageable pageable);

    @NonNull
    Page<Customer> findAll(@NonNull Pageable pageable);

    @NonNull
    Customer save(@NonNull Customer customer);
}
