package com.oreo.insightfactory.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.oreo.insightfactory.sale.Sale;
import com.oreo.insightfactory.sale.SaleRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesAggregationServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @InjectMocks
    private SalesAggregationService salesAggregationService;

    private OffsetDateTime from;
    private OffsetDateTime to;

    @BeforeEach
    void setup() {
        from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        to = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Test
    void shouldCalculateCorrectAggregatesWithValidData() {
        List<Sale> sales = List.of(
                sale("OREO_CLASSIC", 10, new BigDecimal("1.99"), "Miraflores"),
                sale("OREO_DOUBLE", 5, new BigDecimal("2.49"), "San Isidro"),
                sale("OREO_CLASSIC", 15, new BigDecimal("1.99"), "Miraflores")
        );
        when(saleRepository.findWithinRange(from, to, null)).thenReturn(sales);

        SalesAggregates aggregates = salesAggregationService.calculateAggregates(from, to, null);

        assertThat(aggregates.totalUnits()).isEqualTo(30);
        assertThat(aggregates.totalRevenue()).isEqualByComparingTo(new BigDecimal("49.75"));
        assertThat(aggregates.topSku()).isEqualTo("OREO_CLASSIC");
        assertThat(aggregates.topBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldReturnEmptyAggregatesWhenNoSales() {
        when(saleRepository.findWithinRange(from, to, null)).thenReturn(List.of());

        SalesAggregates aggregates = salesAggregationService.calculateAggregates(from, to, null);

        assertThat(aggregates.totalUnits()).isZero();
        assertThat(aggregates.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(aggregates.topSku()).isNull();
        assertThat(aggregates.topBranch()).isNull();
    }

    @Test
    void shouldFilterByBranch() {
        List<Sale> sales = List.of(
                sale("OREO_CLASSIC", 10, new BigDecimal("1.99"), "Miraflores"),
                sale("OREO_CLASSIC", 5, new BigDecimal("1.99"), "San Isidro")
        );
        when(saleRepository.findWithinRange(from, to, "Miraflores")).thenReturn(List.of(sales.get(0)));

        SalesAggregates aggregates = salesAggregationService.calculateAggregates(from, to, "Miraflores");

        assertThat(aggregates.totalUnits()).isEqualTo(10);
        assertThat(aggregates.topBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldFilterByDates() {
        Sale saleInside = sale("OREO_DOUBLE", 12, new BigDecimal("2.49"), "Miraflores");
        when(saleRepository.findWithinRange(from, to, null)).thenReturn(List.of(saleInside));

        SalesAggregates aggregates = salesAggregationService.calculateAggregates(from, to, null);

        assertThat(aggregates.totalUnits()).isEqualTo(12);
        assertThat(aggregates.topSku()).isEqualTo("OREO_DOUBLE");
    }

    @Test
    void shouldHandleSkuTieByAlphabeticalOrder() {
        List<Sale> sales = List.of(
                sale("OREO_A", 10, new BigDecimal("2.00"), "Miraflores"),
                sale("OREO_B", 10, new BigDecimal("2.00"), "Miraflores")
        );
        when(saleRepository.findWithinRange(from, to, null)).thenReturn(sales);

        SalesAggregates aggregates = salesAggregationService.calculateAggregates(from, to, null);

        assertThat(aggregates.topSku()).isEqualTo("OREO_A");
    }

    private Sale sale(String sku, int units, BigDecimal price, String branch) {
        Sale sale = new Sale();
        sale.setSku(sku);
        sale.setUnits(units);
        sale.setPrice(price);
        sale.setBranch(branch);
        sale.setSoldAt(OffsetDateTime.now());
        return sale;
    }
}
