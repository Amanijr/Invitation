package com.InvitationSystem.InvitationSystem.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeyLoggerRunner implements CommandLineRunner {

    @Autowired
    private SecretKeyGenerator keyGenerator;

    @Override
    public void run(String... args) throws Exception {
        log.info("--- SECURE JWT SECRET GENERATED ---");
        log.info(keyGenerator.generateSecureSecret());
    }
}
