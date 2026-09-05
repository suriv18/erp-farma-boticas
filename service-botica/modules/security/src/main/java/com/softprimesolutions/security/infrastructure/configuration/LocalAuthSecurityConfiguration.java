package com.softprimesolutions.security.infrastructure.configuration;

import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import java.time.Clock;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class LocalAuthSecurityConfiguration {

    @Bean
    JwtDecoder localJwtDecoder(
            SecretKey localAuthSecretKey,
            LocalAuthStorePort store,
            Clock iamClock,
            @Value("${security.local-auth.issuer}") String issuer,
            @Value("${security.local-auth.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withSecretKey(localAuthSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", audiences -> audiences != null && audiences.contains(audience));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return new SessionAwareJwtDecoder(decoder, store, iamClock);
    }

    @Bean
    LocalJwtAuthenticationConverter localJwtAuthenticationConverter(
            LocalAuthStorePort store, Clock iamClock) {
        return new LocalJwtAuthenticationConverter(store, iamClock);
    }

    @Bean
    SecurityFilterChain localAuthSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder localJwtDecoder,
            LocalJwtAuthenticationConverter converter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/password/forgot",
                                "/api/v1/auth/password/reset").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(localJwtDecoder)
                        .jwtAuthenticationConverter(converter)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults());
        return http.build();
    }
}
