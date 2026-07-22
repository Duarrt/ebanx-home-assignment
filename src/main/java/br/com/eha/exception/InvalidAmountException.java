package br.com.eha.exception;

public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(int amount) {
        super("Invalid amount: " + amount);
    }
}
