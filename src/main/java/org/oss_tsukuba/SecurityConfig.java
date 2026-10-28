package org.oss_tsukuba;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
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

    @Value("${oidc.id-token-signing-algorithm:ES256}")
    private String idTokenSigningAlgorithm;
    
    @Value("${oidc.end-session-uri:}")
    private String endSessionUri;
    
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

        ClientRegistrationRepository repository = clientRegistrationRepository;

        if (!endSessionUri.isBlank()) {

            ClientRegistration registration =
                    clientRegistrationRepository.findByRegistrationId("oidc");

            Map<String, Object> metadata = new HashMap<>(
                    registration.getProviderDetails().getConfigurationMetadata());

            // Discovery Document で取得できていない場合だけ補完
            metadata.putIfAbsent("end_session_endpoint", endSessionUri);

            ClientRegistration updated =
                    ClientRegistration.withClientRegistration(registration)
                            .providerConfigurationMetadata(metadata)
                            .build();

            repository = new InMemoryClientRegistrationRepository(updated);
        }

        JwtServerLogoutHandler handler =
                new JwtServerLogoutHandler(repository, tokenTimeRepository, userClaim);

        handler.setPostLogoutRedirectUri("{baseUrl}");

        return handler;
    }

    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        OidcIdTokenDecoderFactory factory =
                new OidcIdTokenDecoderFactory();

        factory.setJwsAlgorithmResolver(
                registration -> SignatureAlgorithm.from(idTokenSigningAlgorithm));

        return factory;
    }
}
