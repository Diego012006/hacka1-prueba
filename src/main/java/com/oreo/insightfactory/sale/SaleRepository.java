package com.oreo.insightfactory.sale;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Query("SELECT s FROM Sale s WHERE (:branch IS NULL OR s.branch = :branch) "
            + "AND (:from IS NULL OR s.soldAt >= :from) AND (:to IS NULL OR s.soldAt <= :to)")
    Page<Sale> search(@Param("branch") String branch,
                      @Param("from") OffsetDateTime from,
                      @Param("to") OffsetDateTime to,
                      Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE (:branch IS NULL OR s.branch = :branch) "
            + "AND s.soldAt >= :from AND s.soldAt <= :to")
    List<Sale> findWithinRange(@Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to,
                               @Param("branch") String branch);
}
