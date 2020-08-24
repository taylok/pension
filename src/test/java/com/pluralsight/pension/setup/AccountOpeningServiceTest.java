package com.pluralsight.pension.setup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountOpeningServiceTest {

    private AccountOpeningService underTest;

    @BeforeEach
    void setUp() {
        // Without mockito, we pass nulls to the constructor for each of the class collaborators
        underTest = new AccountOpeningService(null,null,null);
    }

    @Test
    public void shouldOpenAccount() throws IOException {
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount("John","Smith","123xyz9", LocalDate.of(1990,1,1));
        assertEquals(AccountOpeningStatus.OPENED,accountOpeningStatus);
    }
}
