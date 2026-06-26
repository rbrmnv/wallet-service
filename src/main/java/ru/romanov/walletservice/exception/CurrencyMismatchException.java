package ru.romanov.walletservice.exception;

public class CurrencyMismatchException extends RuntimeException{
    public CurrencyMismatchException(String message) {
        super(message);
    }
}
