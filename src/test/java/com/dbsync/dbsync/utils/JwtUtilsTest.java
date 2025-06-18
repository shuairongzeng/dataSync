package com.dbsync.dbsync.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private Authentication authentication;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        
        // 设置测试用的JWT配置
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "mySecretKey123456789012345678901234567890");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000); // 24小时

        // 创建测试用户
        userDetails = new User("testuser", "password", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void testGenerateJwtToken() {
        // When
        String token = jwtUtils.generateJwtToken(authentication);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 50); // JWT tokens are typically quite long
    }

    @Test
    void testGenerateTokenFromUsername() {
        // When
        String token = jwtUtils.generateTokenFromUsername("testuser");

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 50);
    }

    @Test
    void testGetUserNameFromJwtToken() {
        // Given
        String token = jwtUtils.generateJwtToken(authentication);

        // When
        String username = jwtUtils.getUserNameFromJwtToken(token);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void testValidateJwtToken_ValidToken() {
        // Given
        String token = jwtUtils.generateJwtToken(authentication);

        // When
        boolean isValid = jwtUtils.validateJwtToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateJwtToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateJwtToken_EmptyToken() {
        // When
        boolean isValid = jwtUtils.validateJwtToken("");

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateJwtToken_NullToken() {
        // When
        boolean isValid = jwtUtils.validateJwtToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGetExpirationDateFromToken() {
        // Given
        String token = jwtUtils.generateJwtToken(authentication);

        // When
        Date expirationDate = jwtUtils.getExpirationDateFromToken(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date())); // 应该在未来
    }

    @Test
    void testIsTokenExpired_NotExpired() {
        // Given
        String token = jwtUtils.generateJwtToken(authentication);

        // When
        boolean isExpired = jwtUtils.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void testTokenConsistency() {
        // Given
        String username = "testuser";
        String token1 = jwtUtils.generateTokenFromUsername(username);
        String token2 = jwtUtils.generateJwtToken(authentication);

        // When
        String extractedUsername1 = jwtUtils.getUserNameFromJwtToken(token1);
        String extractedUsername2 = jwtUtils.getUserNameFromJwtToken(token2);

        // Then
        assertEquals(username, extractedUsername1);
        assertEquals(username, extractedUsername2);
        assertTrue(jwtUtils.validateJwtToken(token1));
        assertTrue(jwtUtils.validateJwtToken(token2));
    }
}
