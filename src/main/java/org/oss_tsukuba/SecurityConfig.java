package org.oss_tsukuba;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.text.ParseException;

import com.nimbusds.jwt.SignedJWT;

import org.oss_tsukuba.dao.TokenTimeRepository;
import org.oss_tsukuba.oauth2.Oauth2ResultConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig  {

    @Value("${user-claim:}")
    private String userClaim;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private TokenTimeRepository tokenTimeRepository;

    
    
    @Bean
    public SecurityFilterChain web(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                authorize -> authorize
                        .requestMatchers("/css/**", "/img/**", "/js/**",
                            "/jwt", "/chpass", "/init_jwt",
                            "/webjars/**").permitAll()
                        .requestMatchers("/", "/menu*", "/passphrase*",
                            "/errors*", "/issues*").authenticated())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/jwt", "/chpass", "/init_jwt"))
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true));

        DefaultSecurityFilterChain securityFilterChain = http.build();

        securityFilterChain.getFilters().stream()
            .filter(filter -> filter instanceof OAuth2LoginAuthenticationFilter)
                .forEach(filter -> ((OAuth2LoginAuthenticationFilter) filter)
                    .setAuthenticationResultConverter(new Oauth2ResultConverter()));

        return securityFilterChain;
    }

    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {

        JwtServerLogoutHandler handler =
                new JwtServerLogoutHandler(clientRegistrationRepository, tokenTimeRepository, userClaim);

        handler.setPostLogoutRedirectUri("{baseUrl}");

        return handler;
    }

    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        Map<SignatureAlgorithm, OidcIdTokenDecoderFactory> factories = new ConcurrentHashMap<>();
        return registration -> token -> {
            SignatureAlgorithm algorithm;
            try {
                algorithm = SignatureAlgorithm.from(
                        SignedJWT.parse(token).getHeader().getAlgorithm().getName());
            } catch (ParseException e) {
                throw new BadJwtException("Invalid ID Token", e);
            }
            if (algorithm == null) {
                throw new BadJwtException("Unsupported ID Token signing algorithm");
            }

            // Keep Spring's OIDC validators and cache decoders separately for each algorithm.
            OidcIdTokenDecoderFactory factory = factories.computeIfAbsent(algorithm, key -> {
                OidcIdTokenDecoderFactory decoderFactory = new OidcIdTokenDecoderFactory();
                decoderFactory.setJwsAlgorithmResolver(client -> key);
                return decoderFactory;
            });
            return factory.createDecoder(registration).decode(token);
        };
    }
}
