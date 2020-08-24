package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;

import java.io.IOException;
import java.time.LocalDate;

public class AccountOpeningService {

    private static final String UNACCEPTABLE_RISK_PROFILE = "HIGH";
    private AccountOpeningEventPublisher eventPublisher;
    private BackgroundCheckService backgroundCheckService;
    private ReferenceIdsManager referenceIdsManager;
    private AccountRepository accountRepository;


    public <eventPublisher> AccountOpeningService( BackgroundCheckService backgroundCheckService,
                                                   ReferenceIdsManager referenceIdsManager,
                                                   AccountRepository accountRepository,
                                                   AccountOpeningEventPublisher eventPublisher ) {
        this.backgroundCheckService = backgroundCheckService;
        this.referenceIdsManager = referenceIdsManager;
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }


    public AccountOpeningStatus openAccount( String firstName, String lastName, String taxId, LocalDate dob )
            throws IOException {

        final BackgroundCheckResults backgroundCheckResults = backgroundCheckService.confirm(firstName,
                lastName,
                taxId,
                dob);

        if (backgroundCheckResults == null || backgroundCheckResults.getRiskProfile().equals(UNACCEPTABLE_RISK_PROFILE)) {
            return AccountOpeningStatus.DECLINED;
        } else {
            final String id = referenceIdsManager.obtainId(firstName, lastName, taxId, dob);
            if (id != null) {
                accountRepository.save(id, firstName, lastName, taxId, dob, backgroundCheckResults);
                eventPublisher.notify(id);
                return AccountOpeningStatus.OPENED;
            } else {
                return AccountOpeningStatus.DECLINED;
            }
        }
    }
}
