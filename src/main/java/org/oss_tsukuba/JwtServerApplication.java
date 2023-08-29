package org.oss_tsukuba;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.common.crypto.CryptoIntegration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class JwtServerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {

        Security.addProvider(new BouncyCastleProvider());
        CryptoIntegration.init(JwtServerApplication.class.getClassLoader());

        SpringApplication.run(JwtServerApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {

        Security.addProvider(new BouncyCastleProvider());
        CryptoIntegration.init(getClass().getClassLoader());

        return builder.sources(JwtServerApplication.class);
    }
}
