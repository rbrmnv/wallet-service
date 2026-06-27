package ru.romanov.walletservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.romanov.walletservice.dto.SummaryResponse;
import ru.romanov.walletservice.dto.WalletBalanceResponse;
import ru.romanov.walletservice.service.ReportService;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/balance/{walletId}")
    public WalletBalanceResponse getWalletBalance(@PathVariable UUID walletId){
        return service.getWalletBalance(walletId);
    }

    @GetMapping("/summary")
    public SummaryResponse getSummeryReport(){
        return service.getSummeryReport();
    }
}
