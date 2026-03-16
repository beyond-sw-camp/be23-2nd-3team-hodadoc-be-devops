package com.beyond.hodadoc.common.configs;

import com.beyond.hodadoc.common.auth.JwtTokenFilter;
import com.beyond.hodadoc.common.exception.JwtAuthenticationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final JwtAuthenticationHandler jwtAuthenticationHandler;

    @Autowired
    public SecurityConfig(JwtTokenFilter jwtTokenFilter, JwtAuthenticationHandler jwtAuthenticationHandler) {
        this.jwtTokenFilter = jwtTokenFilter;
        this.jwtAuthenticationHandler = jwtAuthenticationHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers(HttpMethod.POST, "/account/create").permitAll()
                        .requestMatchers(HttpMethod.POST, "/account/doLogin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/account/kakao/doLogin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/account/refresh").permitAll() // ✅ 추가
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/hospital/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctor/hospital/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/doctor/*/offdays/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reservation/slots").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reservation/hospital/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/sse/**").permitAll()
                        .requestMatchers("/connect/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/admin/department/list").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/filter/list").permitAll()

                        .requestMatchers(HttpMethod.GET, "/reviews/*/hospitalist").permitAll()
                        .requestMatchers(HttpMethod.GET, "/reviews/*/stats").permitAll()

                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://www.hodadoc.littleniddle.store",
                "http://192.168.*.*:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder pwEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}