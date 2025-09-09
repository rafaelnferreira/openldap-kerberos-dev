package com.example.kerberos.controller;

import com.example.kerberos.model.UserInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/user")
    public ResponseEntity<UserInfo> getUserInfo(Authentication authentication) {
        var userInfo = new UserInfo();
        userInfo.setUsername(authentication.getName());
        userInfo.setPrincipal(authentication.getName());
        userInfo.setGroups(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        userInfo.setRealm("EXAMPLE.ORG");
        userInfo.setAuthenticated(authentication.isAuthenticated());
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/auth/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        Map<String, Object> status = new HashMap<>();
        
        if (authentication != null && authentication.isAuthenticated()) {
            status.put("authenticated", true);
            status.put("principal", authentication.getName());
            status.put("authorities", authentication.getAuthorities());
            status.put("authType", authentication.getClass().getSimpleName());
        } else {
            status.put("authenticated", false);
        }
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a protected endpoint");
        response.put("user", authentication != null ? authentication.getName() : "anonymous");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a public endpoint - no authentication required");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
}
