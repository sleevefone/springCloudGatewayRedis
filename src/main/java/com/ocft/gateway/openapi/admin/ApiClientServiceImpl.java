package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the ApiClientService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiClientServiceImpl implements ApiClientService {

    private final ApiClientRepository apiClientRepository;

    @Override
    public Optional<ApiClient> getByAppKey(String appKey) {
        return apiClientRepository.findByAppKey(appKey);
    }

    @Override
    public List<ApiClient> getAllClients() {
        return apiClientRepository.findAll();
    }

    @Override
    @Transactional
    public ApiClient saveClient(ApiClient client) {
        return apiClientRepository.save(client);
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        apiClientRepository.deleteById(id);
    }
}
