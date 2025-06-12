package digital.pragmatech.demo.service;

import digital.pragmatech.demo.model.User;
import digital.pragmatech.demo.repository.UserRepository;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringTestInsightExtension.class})
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }
    
    @Test
    void testCreateUserSuccess() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        
        User created = userService.createUser("newuser", "new@example.com");
        
        assertNotNull(created);
        assertEquals("newuser", created.getUsername());
        assertEquals("new@example.com", created.getEmail());
        assertEquals(1L, created.getId());
        
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testCreateUserWithExistingUsername() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser("existing", "new@example.com")
        );
        
        assertEquals("Username already exists: existing", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testCreateUserWithExistingEmail() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser("newuser", "existing@example.com")
        );
        
        assertEquals("Email already exists: existing@example.com", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testFindById() {
        User user = new User("testuser", "test@example.com");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        Optional<User> found = userService.findById(1L);
        
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }
    
    @Test
    void testFindByUsername() {
        User user = new User("testuser", "test@example.com");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        
        Optional<User> found = userService.findByUsername("testuser");
        
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }
    
    @Test
    void testFindAll() {
        List<User> users = Arrays.asList(
            new User("user1", "user1@example.com"),
            new User("user2", "user2@example.com")
        );
        when(userRepository.findAll()).thenReturn(users);
        
        List<User> found = userService.findAll();
        
        assertEquals(2, found.size());
    }
    
    @Test
    void testDeleteUser() {
        userService.deleteUser(1L);
        
        verify(userRepository).deleteById(1L);
    }
    
    @Test
    void testGetUserCount() {
        when(userRepository.count()).thenReturn(5L);
        
        long count = userService.getUserCount();
        
        assertEquals(5L, count);
    }
}