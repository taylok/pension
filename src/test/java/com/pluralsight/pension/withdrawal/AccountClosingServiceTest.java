package com.pluralsight.pension.withdrawal;

import com.pluralsight.pension.Account;
import com.pluralsight.pension.setup.BackgroundCheckService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AccountClosingServiceTest {

    @Mock
    private BackgroundCheckService backgroundCheckService;
    // Concept of today and tomorrow are now fixed in time
    Instant fixedTime = LocalDate.of(2020, 8, 26).atStartOfDay(ZoneId.systemDefault()).toInstant();
    Clock clock = Clock.fixed(fixedTime,ZoneId.systemDefault());
    // Unit under test is passed the mock we just created
    private AccountClosingService underTest = new AccountClosingService(backgroundCheckService, clock);

    @Test
    public void shouldDeclineAccountClosingTodayIfHolderReachesRetirementTomorrow() throws IOException {
        Account account = new Account();
        // Ran on 26th Aug 1920
        account.setDob(LocalDate.of(1955, 8, 27));

        final AccountClosingResponse accountClosingResponse = underTest.closeAccount(account);
        assertEquals(AccountClosingStatus.CLOSING_DENIED, accountClosingResponse.getStatus());
        System.out.println(accountClosingResponse.getProcessingDate());
        assertEquals(LocalDateTime.ofInstant(fixedTime, ZoneOffset.systemDefault()),
                accountClosingResponse.getProcessingDate());
    }

}
