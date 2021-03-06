package com.example.security.security.security;

import com.example.security.security.auth.ApplicationUserService;
import com.example.security.security.jwt.JWTUsernameAndPasswordAuthFilter;
import com.example.security.security.jwt.JwtConfig;
import com.example.security.security.jwt.JwtTokenVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;

import static com.example.security.security.security.ApplicationUserPermission.*;
import static com.example.security.security.security.ApplicationUserRoles.*;

@Configuration
@ConfigurationProperties(prefix = "allow")
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final PasswordEncoder passwordEncoder;
    private final ApplicationUserService applicationUserService;
    private final JwtConfig jwtConfig;
    private final SecretKey jwtSecretKey;

//    private final List<String> annoymous = new ArrayList<>();

    @Autowired
    public SecurityConfig(PasswordEncoder passwordEncoder, ApplicationUserService applicationUserService, JwtConfig jwtConfig, SecretKey jwtSecretKey) {
        this.passwordEncoder = passwordEncoder;
        this.applicationUserService = applicationUserService;
        this.jwtConfig = jwtConfig;
        this.jwtSecretKey = jwtSecretKey;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
//                CRSF should only be disabled when the service is not being used by a browser
//                .csrf().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilter(new JWTUsernameAndPasswordAuthFilter(authenticationManager(), jwtConfig, jwtSecretKey))
                //Register the below filter to happen after the above by referencing the name.
                .addFilterAfter(new JwtTokenVerifier(jwtConfig,jwtSecretKey), JWTUsernameAndPasswordAuthFilter.class)
                .authorizeRequests()
                // THE ORDER OF THE MATCHES MATTER.
                .antMatchers("/").permitAll()
                .antMatchers("/api/**").hasRole(STUDENT.name())
                .antMatchers("/management/api/**").hasRole(ADMIN.name())
                // Allows you to add authority to different users
                .antMatchers(HttpMethod.GET, "/students/api/**").hasAuthority(COURSE_READ.name())
                .antMatchers(HttpMethod.GET,"/management/api/**").hasAnyRole(ADMIN.name(),TRAINEE.name())
                .anyRequest()
                .authenticated();
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(daoAuthenticationProvider());
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(applicationUserService);
        return provider;
    }
}
