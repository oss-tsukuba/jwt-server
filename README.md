# JWT server

JWT server is a web application that stores refresh tokens of OAuth2 securely, and provides the access token using [jwt-agent](https://github.com/oss-tsukuba/jwt-agent.git) and [jwt-logon](https://github.com/oss-tsukuba/jwt-logon.git).  Tht JWT server assumes to use Keycloak as an identity provider.

When you log in the JWT server, access information will be provided, which includes a user name and passphrase to retrieve an access token by `jwt-agent` or `jwt-logon`.

JWT servers can be redundant using DBMS multi-source replication.  When `jwt-agent` or `jwt-logon` specifies multiple redundant JWT servers, an active JWT server is automatically selected.

# System Requirements

 - [Keycloak](https://www.keycloak.org/)
 - Web Application Server
 - DBMS (MySQL/MariaDB)

# Build Requirements

- JDK (17 or later)
- Apache Maven (3.2 or later)

# Build jwt-server war file

## Set up property file

Change `src/main/resources/application.properties` for your environment.

### Keycloak settings

Specify the realm and the confidential client on Keycloak.
Change below for the Keycloak.

```
spring.security.oauth2.client.registration.keycloak.client-id=${CLIENT}
spring.security.oauth2.client.registration.keycloak.client-secret=${CONFIDENTIAL}
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://${KEYCLOAK}:8443/auth/realms/${REALM}
```
- spring.security.oauth2.client.provider.keycloak.issuer-uri: Keycloak's authentication url
- spring.security.oauth2.client.registration.keycloak.client-id: Client name
- spring.security.oauth2.client.registration.keycloak.client-secret: Confidential code for the client

### OAuth2 token setting

Specify the claim for user name.
```
user-claim=${USERCLAIM}
```
- user-claim: claim in OAuth2 token for user name

If this setting is missing, `preferred_username` claim is used.

### Database settings

Create a user `${USERNAME}` and a database `${DB}` with `ddl/jwt-server.ddl`.
Change below for your DBMS.

```
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/${DB}
spring.datasource.username=${USERNAME}
spring.datasource.password=${PASSWORD}
```

- spring.datasource.url: URL for the database (i.e. jdbc:mysql://localhost:3306/${DB})
- spring.datasource.username: username for the database
- spring.datasource.password: password for the user

### Other settings (optional)

This is only for development and testing purpose.

```
jwt-server.passphrase=${PASSPHRASE}
```

 - jwt-server.passphrase: Initial passphrase

If you specify this, you can get a passphrase for `${USER}` to retrieve an access token by the following command line using a secret API when the JWT server's URL is `https://${HOST}/jwt-server/`.  This is useful for automatic testing purpose to obtain the passphrase by the command line without a web browser.  This API can be used only once.
```
curl -X POST -d "user=${USER}" -d "password=${PASSWORD}" -d "passphrase=${PASSPHRAE}" https://${HOST}/jwt-server/init_jwt
```
## Build

Build `target/jwt-server.war` with the following command.

```
mvn package
```

# Deploy the JWT server

Deploy `jwt-server.war` on the web application server like tomcat(10 or later).

