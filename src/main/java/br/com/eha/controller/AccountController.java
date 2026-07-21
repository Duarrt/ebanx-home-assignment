package br.com.eha.controller;

import br.com.eha.dto.request.EventRequest;
import br.com.eha.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return accountService.manageEvent(event);
    }
}
