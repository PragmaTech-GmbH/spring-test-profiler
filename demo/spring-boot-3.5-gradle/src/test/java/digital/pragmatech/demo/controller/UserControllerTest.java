package digital.pragmatech.demo.controller;

import digital.pragmatech.demo.model.User;
import digital.pragmatech.demo.service.UserService;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetAllUsers() throws Exception {
        User user1 = new User("user1", "user1@example.com");
        user1.setId(1L);
        User user2 = new User("user2", "user2@example.com");
        user2.setId(2L);
        
        when(userService.findAll()).thenReturn(Arrays.asList(user1, user2));
        
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }
    
    @Test
    void testGetUserByIdFound() throws Exception {
        User user = new User("testuser", "test@example.com");
        user.setId(1L);
        
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    
    @Test
    void testGetUserByIdNotFound() throws Exception {
        when(userService.findById(anyLong())).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testCreateUserSuccess() throws Exception {
        User user = new User("newuser", "new@example.com");
        user.setId(1L);
        
        when(userService.createUser("newuser", "new@example.com")).thenReturn(user);
        
        mockMvc.perform(post("/api/users")
                .param("username", "newuser")
                .param("email", "new@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }
    
    @Test
    void testCreateUserBadRequest() throws Exception {
        when(userService.createUser(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already exists"));
        
        mockMvc.perform(post("/api/users")
                .param("username", "existing")
                .param("email", "existing@example.com"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }
}