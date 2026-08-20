package com.example.enversdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * The whole app now sits behind login, not just the Actuator dashboard. Two things
 * fall out of that:
 *  - {@code /actuator/**} additionally requires the ACTUATOR_ADMIN role, so a
 *    regular user can use the product UI/API but can't see health/env/beans/etc.
 *  - Whoever is logged in becomes available to {@link AuditRevisionListener}, which
 *    stamps their username onto every Envers revision - see that class for how.
 *
 * Three demo users so the audit trail actually has more than one name to show:
 * admin/admin (ACTUATOR_ADMIN + USER), alice/alice and bob/bob (USER only).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/login", "/css/**").permitAll()
                .requestMatchers("/actuator/**", "/actuator.html", "/js/actuator.js").hasRole("ACTUATOR_ADMIN")
                .requestMatchers("/jolokia/**").hasRole("ACTUATOR_ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html", false)
                .failureUrl("/login.html?error")
                .permitAll())
            .logout(logout -> logout
                // GET so a plain <a href="/logout"> link works with CSRF disabled below.
                // AntPathRequestMatcher was removed in Spring Security 7 (Spring Boot 4) -
                // PathPatternRequestMatcher is its replacement.
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
                .logoutSuccessUrl("/login.html?loggedout")
                .permitAll())
            // H2 console renders itself inside an iframe; Spring Security's default
            // X-Frame-Options: DENY would blank it out. Only same-origin frames allowed.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // Disabled for simplicity: login.html is static (no server-side template to
            // stamp a CSRF token into) and this is a local dev tool. Before running this
            // anywhere but localhost, re-enable CSRF and switch the login form to fetch()
            // with CookieCsrfTokenRepository.withHttpOnlyFalse(), or template login.html
            // server-side so a token can be embedded.
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Plaintext-visible-in-config on purpose ({noop}, no hashing) so the three demo
     * logins below are easy to read and try. Swap for a real PasswordEncoder + hashed
     * passwords (or a database-backed UserDetailsService) before this runs anywhere
     * but your own machine.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var admin = User.withUsername("admin")
                .password("{noop}admin")
                .roles("USER", "ACTUATOR_ADMIN")
                .build();
        var alice = User.withUsername("alice")
                .password("{noop}alice")
                .roles("USER")
                .build();
        var bob = User.withUsername("bob")
                .password("{noop}bob")
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(admin, alice, bob);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
