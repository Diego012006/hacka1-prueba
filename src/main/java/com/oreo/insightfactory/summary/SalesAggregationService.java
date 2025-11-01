package com.oreo.insightfactory.summary;

import com.oreo.insightfactory.sale.Sale;
import com.oreo.insightfactory.sale.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SalesAggregationService {

    private final SaleRepository saleRepository;

    public SalesAggregationService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public SalesAggregates calculateAggregates(OffsetDateTime from, OffsetDateTime to, String branch) {
        List<Sale> sales = saleRepository.findWithinRange(from, to, branch);
        if (sales.isEmpty()) {
            return SalesAggregates.empty();
        }

        long totalUnits = sales.stream().mapToLong(Sale::getUnits).sum();
        BigDecimal totalRevenue = sales.stream()
                .map(sale -> sale.getPrice().multiply(BigDecimal.valueOf(sale.getUnits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String topSku = sales.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingLong(Sale::getUnits)))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        String topBranch = sales.stream()
                .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingLong(Sale::getUnits)))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        return new SalesAggregates(totalUnits, totalRevenue, topSku, topBranch);
    }
}
