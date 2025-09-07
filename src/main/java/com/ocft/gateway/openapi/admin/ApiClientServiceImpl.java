package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    public Optional<ApiClient> getClientById(Long id) {
        return apiClientRepository.findById(id);
    }

    @Override
    public List<ApiClient> getAllClients(String query) {
        if (!StringUtils.hasText(query)) {
            return apiClientRepository.findAll();
        }
        return apiClientRepository.findByAppKeyContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
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
