package com.example.auth_service.config;

import com.example.auth_service.entity.AuthProvider;
import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Seeds a single ADMIN account on application startup, so there's always
 * a way to log in as an admin without hand-editing the database (see the
 * comment on AuthService.register() - the /auth/register endpoint always
 * creates CUSTOMER accounts on purpose, this is the other, deliberate
 * path to getting an ADMIN).
 * <p>
 * Runs on every startup but is idempotent: it only inserts a row the
 * first time (keyed on email), so restarting the service never creates
 * duplicates or resets an admin's password after someone has changed it.
 */
@Configuration
@Slf4j
public class AdminSeeder {

    @Value("${admin.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${admin.seed.email:admin@example.com}")
    private String adminEmail;

    @Value("${admin.seed.password:}")
    private String adminPassword;

    @Value("${admin.seed.first-name:System}")
    private String adminFirstName;

    @Value("${admin.seed.last-name:Admin}")
    private String adminLastName;

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!seedEnabled) {
                log.info("Admin seeding disabled (admin.seed.enabled=false), skipping");
                return;
            }

            if (userRepository.existsByEmail(adminEmail)) {
                log.info("Admin user {} already exists, skipping seed", adminEmail);
                return;
            }

            // No default password baked in - a hardcoded fallback here
            // would mean every deployment that forgets to set this ships
            // with the same known admin credentials. Skip instead of
            // guessing.
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("ADMIN_SEED_PASSWORD not set - skipping admin seed. " +
                        "Set admin.seed.password (env ADMIN_SEED_PASSWORD) to create the default admin.");
                return;
            }

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName(adminFirstName)
                    .lastName(adminLastName)
                    .active(true)
                    .role(Role.ADMIN)
                    .provider(AuthProvider.LOCAL)
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);

            log.info("Seeded admin user {}", adminEmail);
        };
    }
}