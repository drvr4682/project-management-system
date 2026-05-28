package com.pms.authservice.controller;

import com.pms.authservice.dto.InternalUserDto;
import com.pms.authservice.entity.User;
import com.pms.authservice.exception.UserNotFoundException;
import com.pms.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<InternalUserDto> getUserInfo(@PathVariable UUID id) {
        log.info("[Internal Auth] Resolving username for user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        InternalUserDto dto = InternalUserDto.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<UUID, String>> getBulkUsernames(@RequestBody List<UUID> ids) {
        log.info("[Internal Auth] Bulk resolving usernames for {} user IDs", ids.size());
        List<User> users = userRepository.findAllById(ids);
        Map<UUID, String> usernameMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getUserName));

        return ResponseEntity.ok(usernameMap);
    }
}
