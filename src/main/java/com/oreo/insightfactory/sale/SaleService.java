package com.oreo.insightfactory.sale;

import com.oreo.insightfactory.sale.dto.SaleRequest;
import com.oreo.insightfactory.sale.dto.SaleResponse;
import com.oreo.insightfactory.security.CurrentUser;
import com.oreo.insightfactory.user.User;
import com.oreo.insightfactory.user.UserRole;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;

    public SaleService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        User user = requireUser();
        validateBranchAccess(user, request.branch());
        Sale sale = new Sale(
                request.sku(),
                request.units(),
                request.price(),
                request.branch(),
                request.soldAt(),
                user
        );
        return SaleResponse.from(saleRepository.save(sale));
    }

    @Transactional(readOnly = true)
    public SaleResponse get(UUID id) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        User user = requireUser();
        validateBranchAccess(user, sale.getBranch());
        return SaleResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> list(OffsetDateTime from, OffsetDateTime to, String branch, Pageable pageable) {
        User user = requireUser();
        String effectiveBranch = branch;
        if (user.getRole() == UserRole.BRANCH) {
            effectiveBranch = user.getBranch();
        }
        Page<Sale> page = saleRepository.search(effectiveBranch, from, to, pageable);
        return page.map(SaleResponse::from);
    }

    @Transactional
    public SaleResponse update(UUID id, SaleRequest request) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        User user = requireUser();
        validateBranchAccess(user, sale.getBranch());
        validateBranchAccess(user, request.branch());
        sale.setSku(request.sku());
        sale.setUnits(request.units());
        sale.setPrice(request.price());
        sale.setBranch(request.branch());
        sale.setSoldAt(request.soldAt());
        return SaleResponse.from(saleRepository.save(sale));
    }

    @Transactional
    public void delete(UUID id) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        saleRepository.delete(sale);
    }

    private void validateBranchAccess(User user, String branch) {
        if (user.getRole() == UserRole.BRANCH && !user.getBranch().equals(branch)) {
            throw new ValidationException("Branch user cannot access another branch");
        }
    }

    private User requireUser() {
        User user = CurrentUser.get();
        if (user == null) {
            throw new ValidationException("User not authenticated");
        }
        return user;
    }
}
