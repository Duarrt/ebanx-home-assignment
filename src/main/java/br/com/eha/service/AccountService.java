package br.com.eha.service;

import br.com.eha.dto.Account;
import br.com.eha.dto.request.EventRequest;
import br.com.eha.dto.response.DepositResponse;
import br.com.eha.dto.response.TransferResponse;
import br.com.eha.dto.response.WithdrawResponse;
import br.com.eha.enums.EventType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class AccountService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    private final Map<EventType, Function<EventRequest, ResponseEntity<Object>>> functions = Map.of(
            EventType.DEPOSIT, this::doDeposit,
            EventType.WITHDRAW, this::doWithdraw,
            EventType.TRANSFER, this::doTransfer
    );

    public void reset(){
        accounts.clear();
    }

    public Integer getBalance(String accountId) {
        Account account = accounts.get(accountId);
        return account != null ? account.getBalance() : null;
    }

    private ResponseEntity<Object> doDeposit(EventRequest event) {
        String destination = event.getDestination();
        Integer amount = event.getAmount();

        Account account = accounts.computeIfAbsent(destination, id -> new Account(id, 0));
        account.deposit(amount);

        int newBalance = account.getBalance();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new DepositResponse(new Account(destination, newBalance)));
    }

    private ResponseEntity<Object> doWithdraw(EventRequest event) {
        String origin = event.getOrigin();
        Integer amount = event.getAmount();

        Account account = accounts.get(origin);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
        }
        if (!account.withdraw(amount)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(0);
        }

        int newBalance = account.getBalance();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new WithdrawResponse(new Account(origin, newBalance)));
    }

    private ResponseEntity<Object> doTransfer(EventRequest event) {
        String destination = event.getDestination();
        String origin = event.getOrigin();
        Integer amount = event.getAmount();

        EventRequest withdrawEvent = new EventRequest();
        withdrawEvent.setType(EventType.WITHDRAW);
        withdrawEvent.setOrigin(origin);
        withdrawEvent.setAmount(amount);

        ResponseEntity<Object> withdrawResponse = doWithdraw(withdrawEvent);
        if (!withdrawResponse.getStatusCode().is2xxSuccessful()) {
            return withdrawResponse;
        }

        Account originAccount = ((WithdrawResponse) withdrawResponse.getBody()).origin();

        EventRequest depositEvent = new EventRequest();
        depositEvent.setType(EventType.DEPOSIT);
        depositEvent.setDestination(destination);
        depositEvent.setAmount(amount);

        ResponseEntity<Object> depositResponse = doDeposit(depositEvent);

        Account destinationAccount = ((DepositResponse) depositResponse.getBody()).destination();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TransferResponse(originAccount, destinationAccount));
    }

    public ResponseEntity<Object> manageEvent(EventRequest event) {
        EventType eventType = event.getType();

        Function<EventRequest, ResponseEntity<Object>> function = functions.get(eventType);

        return function.apply(event);
    }
}