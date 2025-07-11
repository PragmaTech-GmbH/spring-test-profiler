package digital.pragmatech.springtestinsight;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
@ContextConfiguration(classes = JdbcTestExample.JdbcTestConfig.class)
class JdbcTestExample {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    void testDatabaseSetup() {
        assertNotNull(jdbcTemplate);
        
        // Create a test table
        jdbcTemplate.execute("CREATE TABLE test_users (id INT PRIMARY KEY, name VARCHAR(100))");
        
        // Insert test data
        jdbcTemplate.update("INSERT INTO test_users (id, name) VALUES (?, ?)", 1, "John Doe");
        jdbcTemplate.update("INSERT INTO test_users (id, name) VALUES (?, ?)", 2, "Jane Smith");
        
        // Query and verify
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_users", Integer.class);
        assertEquals(2, count);
    }
    
    @Test
    void testJdbcOperations() {
        // Create table if not exists
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS products (id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2))");
        
        // Insert product
        jdbcTemplate.update("INSERT INTO products (id, name, price) VALUES (?, ?, ?)", 1, "Laptop", 999.99);
        
        // Query product
        String productName = jdbcTemplate.queryForObject(
            "SELECT name FROM products WHERE id = ?", 
            String.class, 
            1
        );
        
        assertEquals("Laptop", productName);
    }
    
    @Configuration
    static class JdbcTestConfig {
        
        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
        }
        
        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}