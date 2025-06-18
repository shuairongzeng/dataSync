package com.dbsync.dbsync.service;

import com.dbsync.dbsync.mapper.auth.UserMapper;
import com.dbsync.dbsync.model.User;
import com.dbsync.dbsync.model.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("test@example.com");
        registerRequest.setRole("USER");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setEmail("test@example.com");
        user.setRole("USER");
        user.setEnabled(true);
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        when(userMapper.existsByUsername(anyString())).thenReturn(false);
        when(userMapper.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userMapper.insertUser(any(User.class))).thenReturn(1);

        // When
        User result = userService.registerUser(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("USER", result.getRole());
        assertTrue(result.getEnabled());

        verify(userMapper).existsByUsername("testuser");
        verify(userMapper).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userMapper).insertUser(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameExists() {
        // Given
        when(userMapper.existsByUsername(anyString())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registerRequest);
        });

        assertEquals("用户名已存在: testuser", exception.getMessage());
        verify(userMapper).existsByUsername("testuser");
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void testRegisterUser_EmailExists() {
        // Given
        when(userMapper.existsByUsername(anyString())).thenReturn(false);
        when(userMapper.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registerRequest);
        });

        assertEquals("邮箱已存在: test@example.com", exception.getMessage());
        verify(userMapper).existsByUsername("testuser");
        verify(userMapper).existsByEmail("test@example.com");
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void testFindByUsername() {
        // Given
        when(userMapper.findByUsername("testuser")).thenReturn(user);

        // When
        User result = userService.findByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userMapper).findByUsername("testuser");
    }

    @Test
    void testExistsByUsername() {
        // Given
        when(userMapper.existsByUsername("testuser")).thenReturn(true);

        // When
        boolean result = userService.existsByUsername("testuser");

        // Then
        assertTrue(result);
        verify(userMapper).existsByUsername("testuser");
    }

    @Test
    void testValidatePassword() {
        // Given
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // When
        boolean result = userService.validatePassword("password123", "encodedPassword");

        // Then
        assertTrue(result);
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }
}
