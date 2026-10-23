package com.empmanage.cms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/empmanage_cms_db}")
    private String rawUrl;

    @Value("${spring.datasource.username:postgres}")
    private String rawUsername;

    @Value("${spring.datasource.password:admin}")
    private String rawPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = rawUrl != null ? rawUrl.trim() : "";
        String username = rawUsername;
        String password = rawPassword;

        // Auto-detect and parse Render, Heroku or Railway style connection URIs
        // Format: postgres://user:password@host:port/database or postgresql://user:password@host:port/database
        if (jdbcUrl.startsWith("postgres://") || jdbcUrl.startsWith("postgresql://")) {
            try {
                URI uri = new URI(jdbcUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] userParts = userInfo.split(":", 2);
                    username = userParts[0];
                    password = userParts[1];
                }

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath(); // e.g. /empmanage_cms_db

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                log.info("Successfully converted cloud PostgreSQL URI to JDBC URL: {}", jdbcUrl);
            } catch (Exception e) {
                log.warn("Could not parse cloud PostgreSQL URI ({}), falling back to string replacement: {}", jdbcUrl, e.getMessage());
                if (jdbcUrl.startsWith("postgres://")) {
                    jdbcUrl = "jdbc:postgresql://" + jdbcUrl.substring("postgres://".length());
                } else if (jdbcUrl.startsWith("postgresql://")) {
                    jdbcUrl = "jdbc:postgresql://" + jdbcUrl.substring("postgresql://".length());
                }
            }
        } else if (!jdbcUrl.startsWith("jdbc:")) {
            jdbcUrl = "jdbc:postgresql://" + jdbcUrl;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);

        log.info("Initialized HikariDataSource with target database: {}", jdbcUrl);
        return new HikariDataSource(config);
    }
}
