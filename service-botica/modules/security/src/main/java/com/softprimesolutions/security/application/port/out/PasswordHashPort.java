package com.softprimesolutions.security.application.port.out;

public interface PasswordHashPort {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
