package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountOpeningServiceTest {

    private static final String UNACCEPTABLE_RISK_PROFILE = "HIGH";
    private AccountOpeningService underTest;
    // Call static mock method for all three collaborators
    private BackgroundCheckService backgroundCheckService = mock(BackgroundCheckService.class);
    private ReferenceIdsManager referenceIdsManager = mock(ReferenceIdsManager.class);
    private AccountRepository accountRepository = mock(AccountRepository.class);

    @BeforeEach
    void setUp() {
        // Replace mocks with collaborators
        underTest = new AccountOpeningService(backgroundCheckService,referenceIdsManager,accountRepository);
    }

    @Test
    public void shouldOpenAccount() throws IOException {
        // Won't get away without the mock stubbing here!
        when(backgroundCheckService.confirm("John","Smith","123xyz9", LocalDate.of(1990,1,1)))
                .thenReturn(new BackgroundCheckResults("something not acceptable", 100));
        when(referenceIdsManager.obtainId("John","Smith","123xyz9", LocalDate.of(1990,1,1)))
                .thenReturn("some_Id");
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount("John","Smith","123xyz9", LocalDate.of(1990,1,1));
        assertEquals(AccountOpeningStatus.OPENED,accountOpeningStatus);
    }

    @Test
    public void shouldDeclineAccount() throws IOException {
        when(backgroundCheckService.confirm("John","Smith","123", LocalDate.of(1990,1,1)))
                .thenReturn(new BackgroundCheckResults(UNACCEPTABLE_RISK_PROFILE, 0));
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount("John","Smith","123", LocalDate.of(1990,1,1));
        assertEquals(AccountOpeningStatus.DECLINED,accountOpeningStatus);
    }
}
