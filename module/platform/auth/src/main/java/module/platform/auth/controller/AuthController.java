package module.platform.auth.controller;

import lombok.RequiredArgsConstructor;
import module.platform.security.service.JwtTokenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final JwtTokenService tokenService;

    @PostMapping("/token")
    public Map<String, Object> issue(@RequestBody Map<String,Object> req) throws Exception {
        String subject = (String) req.getOrDefault("sub", "dev-user");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) req.getOrDefault("roles", List.of("CATALOG_READ"));
        String token = tokenService.issue(subject, roles);
        return Map.of("access_token", token, "token_type", "Bearer");
    }
}
