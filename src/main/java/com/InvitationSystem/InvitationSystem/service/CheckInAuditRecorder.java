package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.entity.CheckIn;
import com.InvitationSystem.InvitationSystem.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInAuditRecorder {

    private final CheckInRepository checkInRepository;
    private final PlatformTransactionManager transactionManager;

    public CheckIn record(CheckIn audit) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return template.execute(status -> checkInRepository.saveAndFlush(audit));
        } catch (RuntimeException ex) {
            log.warn("Could not persist check-in audit ({}): {}", audit.getResult(), ex.getMessage());
            return audit;
        }
    }
}
