package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class AccountOpeningServiceTest {

    // Extracted constants from test
    private static final String UNACCEPTABLE_RISK_PROFILE = "HIGH";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final String TAX_ID = "123xyz9";
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);
    private static final String ACCOUNT_ID = "some_Id";
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
        final BackgroundCheckResults okBackgroundCheckResults = new BackgroundCheckResults("something not acceptable",100);
        // Stubbed BackgroundCheckResults. Unit under test asking for this data
        given(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .willReturn(okBackgroundCheckResults);
        // Stubbed ReferenceIdsManager. Unit under test asking for this data
        given(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .willReturn(ACCOUNT_ID);
        // Unit under Test
        final AccountOpeningStatus accountOpeningStatus = underTest.openAccount(FIRST_NAME,LAST_NAME,TAX_ID, DOB);
        assertEquals(AccountOpeningStatus.OPENED,accountOpeningStatus);
        // Verify behaviour of Unit under Test. Tell collaborator to do somthing
        // Argument Captor
        ArgumentCaptor<BackgroundCheckResults> backgroundCheckResultsArgumentCaptor = ArgumentCaptor.forClass(BackgroundCheckResults.class);
        // should() can take params allowing specification of number of invocations
        then(accountRepository).should().save(eq(ACCOUNT_ID),eq(FIRST_NAME),eq(LAST_NAME),eq(TAX_ID), eq(DOB), backgroundCheckResultsArgumentCaptor.capture());
        then(accountOpeningEventPublisher).should().notify(anyString());
        System.out.println(backgroundCheckResultsArgumentCaptor.getValue().getRiskProfile() + " " + backgroundCheckResultsArgumentCaptor.getValue().getUpperAccountLimit());
        // Assert methods were called
        assertEquals(okBackgroundCheckResults.getRiskProfile(), backgroundCheckResultsArgumentCaptor.getValue().getRiskProfile());
        assertEquals(okBackgroundCheckResults.getUpperAccountLimit(), backgroundCheckResultsArgumentCaptor.getValue().getUpperAccountLimit());
        // Verify no unverified interactions with BackgroundCheck service mock
        verifyNoMoreInteractions(ignoreStubs(backgroundCheckService, referenceIdsManager));
        // verifyNoMoreInteractions(accountRepository, accountOpeningEventPublisher);
        then(accountRepository).shouldHaveNoMoreInteractions();
        then(accountOpeningEventPublisher).shouldHaveNoMoreInteractions();
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

    @Test
    public void shouldThrowIfBackgroundChecksServiceThrows() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenThrow(new IOException());
        assertThrows(IOException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));
    }

    @Test
    public void shouldThrowIfReferenceIdsManagerThrows() throws IOException {
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(new BackgroundCheckResults("something_not_unacceptable", 100));
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));
    }

    @Test
    public void shouldThrowIfAccountRepositoryThrows() throws IOException {
        final BackgroundCheckResults backgroundCheckResults = new BackgroundCheckResults("something_not_unacceptable", 100);
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(backgroundCheckResults);
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenReturn("someID");
        when(accountRepository.save("someID", FIRST_NAME, LAST_NAME, TAX_ID, DOB, backgroundCheckResults))
                .thenThrow(new RuntimeException());
        assertThrows(RuntimeException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));
    }

    @Test
    public void shouldThrowIfEventPublisherThrows() throws IOException {
        final BackgroundCheckResults backgroundCheckResults = new BackgroundCheckResults("something_not_unacceptable", 100);
        when(backgroundCheckService.confirm(FIRST_NAME, LAST_NAME, TAX_ID, DOB))
                .thenReturn(backgroundCheckResults);
        final String accountId = "someID";
        when(referenceIdsManager.obtainId(eq(FIRST_NAME), anyString(), eq(LAST_NAME), eq(TAX_ID), eq(DOB)))
                .thenReturn(accountId);
        when(accountRepository.save(accountId, FIRST_NAME, LAST_NAME, TAX_ID, DOB, backgroundCheckResults))
                .thenReturn(true);
        doThrow(new RuntimeException()).when(accountOpeningEventPublisher).notify(accountId);
        assertThrows(RuntimeException.class, () -> underTest.openAccount(FIRST_NAME, LAST_NAME, TAX_ID, DOB));
    }
}
