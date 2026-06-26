package ru.romanov.walletservice.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.romanov.walletservice.dto.TransactionRequest;
import ru.romanov.walletservice.dto.TransactionResponse;
import ru.romanov.walletservice.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@AllArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request){
        TransactionResponse response = service.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
