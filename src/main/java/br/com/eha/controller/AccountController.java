package br.com.eha.controller;

import br.com.eha.dto.Account;
import br.com.eha.dto.TransferResult;
import br.com.eha.dto.request.EventRequest;
import br.com.eha.dto.response.DepositResponse;
import br.com.eha.dto.response.TransferResponse;
import br.com.eha.dto.response.WithdrawResponse;
import br.com.eha.enums.EventType;
import br.com.eha.exception.AccountNotFoundException;
import br.com.eha.exception.InsufficientFundsException;
import br.com.eha.exception.InvalidAmountException;
import br.com.eha.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping(path = "/reset")
    public ResponseEntity<String> reset() {
        accountService.reset();

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("OK");
    }

    @GetMapping("/balance")
    public ResponseEntity<Integer> getBalance(@RequestParam("account_id") String accountId) {
        Integer balance = accountService.getBalance(accountId);
        if (balance == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
        }
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/event")
    public ResponseEntity<Object> manageEvent(@RequestBody EventRequest event) {
        final String destination = event.getDestination();
        final Integer amount = event.getAmount();
        final String origin = event.getOrigin();

        EventType type = event.getType();
        return switch (type) {
            case DEPOSIT -> {
                Account account = accountService.deposit(destination, amount);
                yield ResponseEntity.status(HttpStatus.CREATED).body(new DepositResponse(account));
            }
            case WITHDRAW -> {
                Account account = accountService.withdraw(origin, amount);
                yield ResponseEntity.status(HttpStatus.CREATED).body(new WithdrawResponse(account));
            }
            case TRANSFER -> {
                TransferResult result = accountService.transfer(origin, destination, amount);
                yield ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(result.origin(), result.destination()));
            }
        };
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Object> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Object> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(0);
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<Object> handleInvalidAmount(InvalidAmountException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(0);
    }
}
