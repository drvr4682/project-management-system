package com.pms.authservice.service.verification;

import com.pms.authservice.entity.User;
import com.pms.authservice.entity.VerificationToken;

public interface VerificationService {
    VerificationToken createVerificationToken(User user);
    VerificationToken validateVerificationToken(String token);
}
