package module.platform.security.config;

import module.platform.security.filter.JwtAuthFilter;
import module.platform.security.service.JwtTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(SecurityProps.class)
@EnableMethodSecurity // @PreAuthorize 활성화
public class SecurityConfig {

    @Bean
    public JwtTokenService jwtTokenService(SecurityProps props) {
        return new JwtTokenService(props);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtTokenService svc) {
        return new JwtAuthFilter(svc);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenService svc, JwtAuthFilter jwtAuthFilter) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(
                org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(new JwtAuthFilter(svc),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/**",
                        "/actuator/**",
                        "/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**",
                        "/entity/health"
                ).permitAll()
                .requestMatchers("/product/**").authenticated()
                .requestMatchers("/entity/**").authenticated()

                .requestMatchers("/member/**").permitAll()

                .anyRequest().denyAll()
        );
        return http.build();
    }
}
