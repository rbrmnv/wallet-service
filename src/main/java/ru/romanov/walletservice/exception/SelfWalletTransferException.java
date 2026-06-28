package ru.romanov.walletservice.exception;

public class SelfWalletTransferException extends RuntimeException {
    public SelfWalletTransferException(String message) {
        super(message);
    }
}