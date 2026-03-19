package j2ee_backend.nhom05.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/preorders",
                    "/api/products/**",
                    "/api/categories/**",
                    "/api/brands/**",
                    "/api/carousel/**",
                    "/api/vnpay/**",
                    "/api/momo/**",
                    "/api/attribute-groups/**",
                    "/api/attribute-definitions/**",
                    "/api/category-attributes/**",
                    "/api/sale-programs/active",
                    "/api/sale-programs/*",
                    "/api/vouchers/active",
                    "/api/cart/**",
                    "/api/sse/**",
                    "/images/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/vouchers/validate").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/sale-programs", "/api/vouchers", "/api/vouchers/**")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/sale-programs", "/api/vouchers")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/sale-programs/**", "/api/vouchers/**")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/sale-programs/**", "/api/vouchers/**")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/sale-programs/**", "/api/vouchers/**")
                    .hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                // /api/orders/** yêu cầu xác thực JWT
                .requestMatchers("/api/orders", "/api/orders/**").authenticated()
                // /api/users/** yêu cầu xác thực JWT
                .requestMatchers("/api/users", "/api/users/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
