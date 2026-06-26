package ru.romanov.walletservice.exception;

public class NotEnoughAmountException extends RuntimeException{
    public NotEnoughAmountException(String message) {
        super(message);
    }
}
