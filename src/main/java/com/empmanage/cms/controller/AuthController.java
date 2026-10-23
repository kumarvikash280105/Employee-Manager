package com.empmanage.cms.controller;

import com.empmanage.cms.dto.ApiResponse;
import com.empmanage.cms.dto.LoginRequest;
import com.empmanage.cms.dto.RegisterRequest;
import com.empmanage.cms.entity.User;
import com.empmanage.cms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.register(req);
        user.setPassword(null); // Never expose password hash
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful for " + user.getFullName() + "! Please log in.", user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        User user = userService.login(req);

        // Establish proper Spring Security Authentication in SecurityContext
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole()));
        if (user.isAdmin() && !user.getRole().equals("ROLE_ADMIN")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (user.getRoleEntity() != null && user.getRoleEntity().getPermissions() != null) {
            user.getRoleEntity().getPermissions().forEach(p -> 
                authorities.add(new SimpleGrantedAuthority(p.getName()))
            );
        }
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Bind SecurityContext to HTTP Session
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        session.setAttribute("CURRENT_USER", user);

        user.setPassword(null);
        return ResponseEntity.ok(ApiResponse.ok("Login successful. Welcome back, " + user.getFullName() + "!", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("CURRENT_USER") != null) {
            User sessionUser = (User) session.getAttribute("CURRENT_USER");
            java.util.Optional<User> freshUserOpt = userService.findByEmail(sessionUser.getEmail());
            if (freshUserOpt.isPresent()) {
                User u = freshUserOpt.get();
                u.setPassword(null);
                session.setAttribute("CURRENT_USER", u);

                // Synchronize authorities dynamically in active Spring Security session
                java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
                authorities.add(new SimpleGrantedAuthority(u.getRole()));
                if (u.isAdmin() && !u.getRole().equals("ROLE_ADMIN")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }
                if (u.getRoleEntity() != null && u.getRoleEntity().getPermissions() != null) {
                    u.getRoleEntity().getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
                }
                Authentication newAuth = new UsernamePasswordAuthenticationToken(u.getEmail(), null, authorities);
                SecurityContext sc = SecurityContextHolder.createEmptyContext();
                sc.setAuthentication(newAuth);
                SecurityContextHolder.setContext(sc);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

                return ResponseEntity.ok(ApiResponse.ok("Current active session found.", u));
            }
            sessionUser.setPassword(null);
            return ResponseEntity.ok(ApiResponse.ok("Current active session found.", sessionUser));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userService.findByEmail(auth.getName())
                    .map(u -> {
                        u.setPassword(null);
                        return ResponseEntity.ok(ApiResponse.ok("Authenticated user found.", u));
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Session expired.")));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated."));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully.", users));
    }

    @DeleteMapping("/users/{email}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String email) {
        userService.deleteUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("User account removed successfully."));
    }

    @PutMapping("/users/{email}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable String email,
            @RequestBody java.util.Map<String, String> body) {
        String role = body.get("role");
        String department = body.get("department");
        String title = body.get("title");
        User updated = userService.updateUser(email, role, department, title);
        updated.setPassword(null);
        return ResponseEntity.ok(ApiResponse.ok("User account updated successfully.", updated));
    }
}
