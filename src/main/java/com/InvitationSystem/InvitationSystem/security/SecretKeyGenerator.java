package com.InvitationSystem.InvitationSystem.security;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
public class SecretKeyGenerator {

    public String generateSecureSecret(){
        SecretKey key = Jwts.SIG.HS256.key().build();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
