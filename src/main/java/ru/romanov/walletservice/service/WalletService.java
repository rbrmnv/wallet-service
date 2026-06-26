package ru.romanov.walletservice.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.romanov.walletservice.dto.WalletResponse;
import ru.romanov.walletservice.mapper.WalletMapper;
import ru.romanov.walletservice.repository.WalletRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository repository;
    private final WalletMapper mapper = Mappers.getMapper(WalletMapper.class);

    @Transactional
    public List<WalletResponse> getAll(){
        return mapper.toResponseList(repository.findAll());
    }
}
