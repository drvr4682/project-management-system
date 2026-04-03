package com.pms.projectservice.client;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestTemplate restTemplate;

    private static final String AUTH_SERVICE_URL = "http://localhost:8081/api/v1/auth/users/";

    public boolean userExists(String userId) {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(AUTH_SERVICE_URL + userId, String.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException.NotFound e) {
            return false; // user not found

        } catch (ResourceAccessException e) {
            throw new RuntimeException("Auth service unavailable");
        } catch (Exception e) {
            throw new RuntimeException("Error calling auth service");
        }
    }
}
