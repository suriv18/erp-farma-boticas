package com.softprimesolutions.security.infrastructure.client.identity;

import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import java.util.Map;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

public final class SpringPasswordHashAdapter implements PasswordHashPort {

    private static final String ENCODER_ID = "pbkdf2@SpringSecurity_v5_8";
    private final PasswordEncoder delegate;

    public SpringPasswordHashAdapter() {
        this.delegate = new DelegatingPasswordEncoder(
                ENCODER_ID, Map.of(ENCODER_ID, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
