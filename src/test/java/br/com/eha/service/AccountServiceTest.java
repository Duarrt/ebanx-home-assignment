package br.com.eha.service;

import br.com.eha.dto.Account;
import br.com.eha.dto.TransferResult;
import br.com.eha.exception.AccountNotFoundException;
import br.com.eha.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceTest {

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    @Nested
    class Deposit {

        @Test
        void createsAccountWhenItDoesNotExist() {
            Account account = service.deposit("100", 10);

            assertThat(account.getId()).isEqualTo("100");
            assertThat(account.getBalance()).isEqualTo(10);
            assertThat(service.getBalance("100")).isEqualTo(10);
        }

        @Test
        void addsToExistingBalance() {
            service.deposit("100", 10);

            Account account = service.deposit("100", 15);

            assertThat(account.getBalance()).isEqualTo(25);
            assertThat(service.getBalance("100")).isEqualTo(25);
        }
    }

    @Nested
    class Withdraw {

        @Test
        void reducesBalanceOnSuccess() {
            service.deposit("100", 20);

            Account account = service.withdraw("100", 5);

            assertThat(account.getBalance()).isEqualTo(15);
            assertThat(service.getBalance("100")).isEqualTo(15);
        }

        @Test
        void throwsWhenAccountDoesNotExist() {
            assertThatThrownBy(() -> service.withdraw("404", 5))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        void throwsWhenBalanceIsInsufficient() {
            service.deposit("100", 10);

            assertThatThrownBy(() -> service.withdraw("100", 50))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        void doesNotChangeBalanceWhenInsufficient() {
            service.deposit("100", 10);

            assertThatThrownBy(() -> service.withdraw("100", 50))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(service.getBalance("100")).isEqualTo(10);
        }
    }

    @Nested
    class Transfer {

        @Test
        void movesAmountFromOriginToDestination() {
            service.deposit("100", 20);

            TransferResult result = service.transfer("100", "300", 15);

            assertThat(result.origin().getBalance()).isEqualTo(5);
            assertThat(result.destination().getBalance()).isEqualTo(15);
            assertThat(service.getBalance("100")).isEqualTo(5);
            assertThat(service.getBalance("300")).isEqualTo(15);
        }

        @Test
        void addsToExistingDestination() {
            service.deposit("100", 20);
            service.deposit("300", 5);

            service.transfer("100", "300", 10);

            assertThat(service.getBalance("100")).isEqualTo(10);
            assertThat(service.getBalance("300")).isEqualTo(15);
        }

        @Test
        void throwsAndMovesNothingWhenOriginDoesNotExist() {
            assertThatThrownBy(() -> service.transfer("404", "300", 15))
                    .isInstanceOf(AccountNotFoundException.class);

            assertThat(service.getBalance("300")).isNull();
        }

        @Test
        void throwsAndMovesNothingWhenBalanceIsInsufficient() {
            service.deposit("100", 10);

            assertThatThrownBy(() -> service.transfer("100", "300", 50))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(service.getBalance("100")).isEqualTo(10);
            assertThat(service.getBalance("300")).isNull();
        }
    }

    @Nested
    class Balance {

        @Test
        void returnsBalanceForExistingAccount() {
            service.deposit("100", 10);

            assertThat(service.getBalance("100")).isEqualTo(10);
        }

        @Test
        void returnsNullForNonExistingAccount() {
            assertThat(service.getBalance("404")).isNull();
        }

        @Test
        void doesNotChangeStateWhenRead() {
            service.deposit("100", 10);

            service.getBalance("100");
            service.getBalance("100");

            assertThat(service.getBalance("100")).isEqualTo(10);
        }
    }

    @Nested
    class Reset {

        @Test
        void clearsAllAccounts() {
            service.deposit("100", 10);
            service.deposit("200", 20);

            service.reset();

            assertThat(service.getBalance("100")).isNull();
            assertThat(service.getBalance("200")).isNull();
        }
    }
}
