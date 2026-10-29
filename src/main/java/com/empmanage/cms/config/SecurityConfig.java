package com.empmanage.cms.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:8090,http://127.0.0.1:8090,http://localhost:3000,http://127.0.0.1:5500}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri != null && uri.contains(".html")) {
                        response.sendRedirect(request.getContextPath() + "/index.html?error=unauthorized");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please log in.\"}");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    if (uri != null && uri.contains(".html")) {
                        response.sendRedirect(request.getContextPath() + "/index.html?error=forbidden");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Access denied. Administrator privileges required.\"}");
                    }
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public static assets & root
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/css/**",
                    "/js/**",
                    "/assets/**",
                    "/favicon.ico"
                ).permitAll()

                // Public authentication endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/logout",
                    "/api/auth/me"
                ).permitAll()

                // Public read endpoints for home page & catalog
                .requestMatchers(HttpMethod.GET, "/api/cms", "/api/cms/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/departments", "/api/departments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/employees", "/api/employees/**").permitAll()

                // Public contact form inquiry submission
                .requestMatchers(HttpMethod.POST, "/api/contacts").permitAll()

                // Authenticated user attendance actions
                .requestMatchers("/api/attendance/punch").hasAnyRole("USER", "ADMIN", "HR_MANAGER", "MANAGER", "VIEWER")
                .requestMatchers("/api/attendance/my").hasAnyRole("USER", "ADMIN", "HR_MANAGER", "MANAGER", "VIEWER")
                .requestMatchers("/api/attendance/today").hasAnyRole("USER", "ADMIN", "HR_MANAGER", "MANAGER", "VIEWER")
                .requestMatchers("/api/attendance/user/**").authenticated()

                // Role & Permission Management
                .requestMatchers("/roles-permissions.html").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGE")
                .requestMatchers("/api/roles/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGE")
                .requestMatchers("/api/permissions/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGE")

                // Task Management HTML Dashboards & API Endpoints
                .requestMatchers("/employee-tasks.html").authenticated()
                .requestMatchers("/manage-tasks.html").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "TASK_VIEW_ALL", "TASK_CREATE", "TASK_REVIEW")
                .requestMatchers("/api/tasks/my", "/api/tasks/my/**").authenticated()
                .requestMatchers("/api/tasks/all").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "ROLE_VIEWER", "TASK_VIEW_ALL")
                .requestMatchers("/api/tasks/analytics").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR_MANAGER", "ROLE_VIEWER", "TASK_ANALYTICS_VIEW")
                .requestMatchers("/api/tasks/*/review").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "TASK_REVIEW")
                .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER", "TASK_DELETE")
                .requestMatchers("/api/tasks/**").authenticated()

                // Management routes & pages with granular permissions
                .requestMatchers("/admin.html").hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGE")
                .requestMatchers("/api/stats/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_MANAGER", "REPORT_VIEW")
                // Reports & Export
                .requestMatchers("/api/reports/employees/csv").authenticated()
                .requestMatchers("/api/reports/attendance/csv").authenticated()
                .requestMatchers("/api/reports/attendance/user/csv").authenticated()
                .requestMatchers("/api/reports/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR_MANAGER", "REPORT_EXPORT")

                .requestMatchers("/api/attendance/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_MANAGER", "ATTENDANCE_VIEW", "ATTENDANCE_EDIT")
                .requestMatchers(HttpMethod.GET, "/api/contacts/**").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/contacts/**").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/contacts/**").hasAnyAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/departments/**").hasAnyAuthority("ROLE_ADMIN", "DEPARTMENT_CREATE")
                .requestMatchers(HttpMethod.PUT, "/api/departments/**").hasAnyAuthority("ROLE_ADMIN", "DEPARTMENT_EDIT")
                .requestMatchers(HttpMethod.DELETE, "/api/departments/**").hasAnyAuthority("ROLE_ADMIN", "DEPARTMENT_DELETE")

                // Workforce & Employees: Enforced dynamically by EmployeeController via granular permissions
                .requestMatchers(HttpMethod.POST, "/api/employees", "/api/employees/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/employees", "/api/employees/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/employees", "/api/employees/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cms/**").hasAnyAuthority("ROLE_ADMIN", "CMS_EDIT")
                .requestMatchers(HttpMethod.PUT, "/api/cms/**").hasAnyAuthority("ROLE_ADMIN", "CMS_EDIT")
                .requestMatchers(HttpMethod.DELETE, "/api/cms/**").hasAnyAuthority("ROLE_ADMIN", "CMS_EDIT")
                .requestMatchers("/api/auth/users", "/api/auth/users/**").hasAnyAuthority("ROLE_ADMIN", "USER_VIEW", "USER_MANAGE")

                // Any other endpoint requires authentication
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

