package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountOpeningServiceTest {

    // Extracted constants from test
    private static final String UNACCEPTABLE_RISK_PROFILE = "HIGH";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final String TAX_ID = "123xyz9";
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);
    private AccountOpeningService underTest;
    // Call static mock method for all three collaborators
    private BackgroundCheckService backgroundCheckService = mock(BackgroundCheckService.class);
    private ReferenceIdsManager referenceIdsManager = mock(ReferenceIdsManager.class);
    private AccountRepository accountRepository = mock(AccountRepository.class);
    private AccountOpeningEventPublisher accountOpeningEventPublisher= mock(AccountOpeningEventPublisher.class);

    @BeforeEach
    void setUp() {
        // Replace mocks with collaborators
        underTest = new AccountOpeningService(backgroundCheckService,referenceIdsManager,accountRepository, accountOpeningEventPublisher);
    }

    @Test
    public void shouldOpenAccount() throws IOException {
        // Won't get away without the mock stubbing here!
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(new BackgroundCheckResults("something not acceptable", 100));
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenReturn("some_Id");
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME,LAST_NAME,TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.OPENED,accountOpeningStatus);
    }

    @Test
    public void shouldDeclineAccountIfUnacceptableRiskProfileBackgroundCheckResponseReceived() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(new BackgroundCheckResults(UNACCEPTABLE_RISK_PROFILE, 0));
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME,LAST_NAME,TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.DECLINED,accountOpeningStatus);
    }

    @Test
    public void shouldDeclineAccountIfNullBackgroundCheckResponseReceived() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(null);
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }
}
