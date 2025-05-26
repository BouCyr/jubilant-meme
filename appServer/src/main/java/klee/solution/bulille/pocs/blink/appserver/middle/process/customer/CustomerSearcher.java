package klee.solution.bulille.pocs.blink.appserver.middle.process.customer;

import klee.solution.bulille.pocs.blink.appserver.in.http.dtos.outputs.CustomerOutput;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

@FunctionalInterface
public interface CustomerSearcher {
    Page<CustomerOutput> searchCustomers(@NonNull String nameQuery, @NonNull Pageable pageable);
}
