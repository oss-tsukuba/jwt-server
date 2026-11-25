# JWT server

JWT server is a web application that stores refresh tokens of OAuth2 securely, and provides the access token using [jwt-agent](https://github.com/oss-tsukuba/jwt-agent.git) and [jwt-logon](https://github.com/oss-tsukuba/jwt-logon.git). The JWT server assumes the use of Keycloak as an OpenID Connect (OIDC) identity provider.

When you log in to the JWT server, access information will be provided, which includes a user name and passphrase to retrieve an access token by `jwt-agent` or `jwt-logon`.

JWT servers can be redundant using DBMS multi-source replication.  When `jwt-agent` or `jwt-logon` specifies multiple redundant JWT servers, an active JWT server is automatically selected.

# System Requirements

 - [Keycloak](https://www.keycloak.org/)
 - Web Application Server
 - DBMS (MySQL/MariaDB)

# Build Requirements

- JDK (21 or later)
- Apache Maven (3.2 or later)

# Build jwt-server war file

## Set up property file

Change `src/main/resources/application.properties` for your environment.

### Keycloak settings (OIDC)

Specify the realm and confidential client on Keycloak. Use `oidc` as both the registration ID and provider ID because the application looks up this registration by name.

Configuration example:

```properties
spring.security.oauth2.client.registration.oidc.client-id=${CLIENT}
spring.security.oauth2.client.registration.oidc.client-secret=${SECRET}
spring.security.oauth2.client.registration.oidc.provider=oidc
spring.security.oauth2.client.registration.oidc.scope=openid
spring.security.oauth2.client.registration.oidc.authorization-grant-type=authorization_code
spring.security.oauth2.client.provider.oidc.issuer-uri=https://${KEYCLOAK}:443/auth/realms/${REALM}
```

- `client-id`: Client ID registered with Keycloak
- `client-secret`: Secret for the client
- `provider`: `oidc`
- `scope`: Comma-separated scopes, including `openid`. Add any scopes required by your Keycloak configuration or application.
- `authorization-grant-type`: `authorization_code`
- `issuer-uri`: Keycloak's issuer URI, including the realm. Use the path for your deployment (which may omit `/auth`). The application obtains endpoint URLs, including the token endpoint, through OIDC discovery.

When upgrading, rename `spring.security.oauth2.client.registration.keycloak.*` to `spring.security.oauth2.client.registration.oidc.*` and `spring.security.oauth2.client.provider.keycloak.*` to `spring.security.oauth2.client.provider.oidc.*`, and change the registration's `provider` value from `keycloak` to `oidc`.

### OAuth2 token setting

Specify the claim for the user name.

```
user-claim=${USERCLAIM}
```
- user-claim: claim in the OAuth2 token for the user name

If this setting is missing, the `preferred_username` claim is used.

### Database settings

Create a user `${USERNAME}` and a database `${DB}` with `ddl/jwt-server.ddl`.
Change the following for your DBMS.

```
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/${DB}
spring.datasource.username=${USERNAME}
spring.datasource.password=${PASSWORD}
```

- spring.datasource.url: URL for the database (e.g. jdbc:mysql://localhost:3306/${DB})
- spring.datasource.username: username for the database
- spring.datasource.password: password for the user

When upgrading from 1.2.0 or earlier, extend the column lengths of the existing tables, since re-running `ddl/jwt-server.ddl` does not change existing tables. Stop the JWT server and back up the database before running the following.

```sql
ALTER TABLE `errors`
  MODIFY `user` varchar(255) NOT NULL;
ALTER TABLE `tokens`
  MODIFY `user` varchar(255) NOT NULL,
  MODIFY `audience` varchar(255) NOT NULL,
  MODIFY `iv` varchar(255) NOT NULL;
ALTER TABLE `token_time`
  MODIFY `user` varchar(255) NOT NULL;
```

### Other settings (optional)

This is only for development and testing purposes.

```
jwt-server.passphrase=${PASSPHRASE}
```

 - jwt-server.passphrase: Initial passphrase

If you specify this, you can get a passphrase for `${USER}` to retrieve an access token with the following command using a secret API when the JWT server's URL is `https://${HOST}/jwt-server/`.  This is useful for automated testing, where the passphrase needs to be obtained from the command line without a web browser.  This API can be used only once.

```
curl -X POST -d "user=${USER}" -d "password=${PASSWORD}" -d "passphrase=${PASSPHRASE}" https://${HOST}/jwt-server/init_jwt
```

## Build

Build `target/jwt-server.war` with the following command.

```
mvn package
```

# Deploy the JWT server

Deploy `jwt-server.war` on the web application server such as Tomcat (10 or later).
