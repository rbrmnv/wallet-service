package ru.romanov.walletservice.exception;

public class CurrencyMistakeException extends RuntimeException{
    public CurrencyMistakeException(String message) {
        super(message);
    }
}
