package br.com.eha.exception;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(Integer amount) {
        super("Invalid amount: " + amount);
    }
}
