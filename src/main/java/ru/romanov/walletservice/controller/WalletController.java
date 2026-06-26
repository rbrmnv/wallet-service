package ru.romanov.walletservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.romanov.walletservice.dto.WalletResponse;
import ru.romanov.walletservice.service.WalletService;

import java.util.List;

@RequestMapping("/api/wallets")
@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    @GetMapping
    public List<WalletResponse> getAll(){
        return service.getAll();
    }

}
