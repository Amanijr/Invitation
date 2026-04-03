package com.InvitationSystem.InvitationSystem.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class KeyLoggerRunner implements CommandLineRunner {

    @Autowired
    private SecretKeyGenerator keyGenerator;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- NEW SECURE JWT SECRET ---");
        System.out.println(keyGenerator.generateSecureSecret());
    }
}
