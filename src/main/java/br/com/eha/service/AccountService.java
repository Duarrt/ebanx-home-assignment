package br.com.eha.service;

import br.com.eha.dto.Account;
import br.com.eha.dto.TransferResult;
import br.com.eha.exception.AccountNotFoundException;
import br.com.eha.exception.InsufficientFundsException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public void reset() {
        accounts.clear();
    }

    public Integer getBalance(String accountId) {
        Account account = accounts.get(accountId);
        return account != null ? account.getBalance() : null;
    }

    public Account deposit(String destination, int amount) {
        Account account = accounts.computeIfAbsent(destination, id -> new Account(id, 0));
        account.deposit(amount);
        return account;
    }

    public Account withdraw(String origin, int amount) {
        Account account = accounts.get(origin);
        if (account == null) {
            throw new AccountNotFoundException(origin);
        }
        if (!account.withdraw(amount)) {
            throw new InsufficientFundsException(origin);
        }
        return account;
    }

    public TransferResult transfer(String origin, String destination, int amount) {
        Account from = withdraw(origin, amount);
        Account to = deposit(destination, amount);
        return new TransferResult(from, to);
    }
}
