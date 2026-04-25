package com.policyserve.config;

import com.policyserve.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Policy Servicing backend.
 *
 * Authentication: JWT (stateless). Users are currently stored in-memory.
 * Replace InMemoryUserDetailsManager with a DB-backed UserDetailsService
 * once Oracle user tables are available.
 *
 * Default credentials (development only — change in production):
 *   admin   / password  → role ADMIN
 *   viewer  / password  → role VIEWER
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * In-memory user store with two default users.
     * JwtAuthenticationFilter uses this to reload the user after token validation.
     *
     * Replace with a JPA-backed UserDetailsService when a USERS table is available.
     */
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        PasswordEncoder encoder = passwordEncoder();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("admin")
                .password(encoder.encode("password"))
                .roles("ADMIN")
                .build());
        manager.createUser(User.withUsername("viewer")
                .password(encoder.encode("password"))
                .roles("VIEWER")
                .build());
        return manager;
    }

    /** Expose AuthenticationManager so AuthController can inject it. */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }

    // ── HTTP Security ─────────────────────────────────────────────────────────

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // Public: login + health + Swagger
                .antMatchers("/api/v1/auth/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/v2/api-docs", "/configuration/ui", "/swagger-resources/**",
                        "/configuration/security", "/swagger-ui.html", "/webjars/**").permitAll()
                // Preflight
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Everything else requires a valid JWT
                .antMatchers("/api/**").authenticated()
            .and()
            .httpBasic().disable()
            .formLogin().disable()
            // JWT filter — validates Bearer token on every protected request
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
