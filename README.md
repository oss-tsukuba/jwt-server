# JWT server

JWT server is a web application that stores refresh tokens for OAuth2 securely, and provides the access token using [jwt-agent](https://github.com/oss-tsukuba/jwt-agent.git) and [jwt-logon](https://github.com/oss-tsukuba/jwt-logon.git).

# System Requirements

 - Keycloak
 - Web Application Server
 - DBMS (MySQL/MariaDB)

# Build Requirements

- jdk (11 later)
- mvn (3.2 later)

# Building war

## setup property file

Change application.properties for your environments.

### Keycloak settings

Setup the realm and the confidential client on Keycloak.
Change bellows for the Keycloak.

```
keycloak.auth-server-url=https://${KEYCLOAK}:8443/auth
keycloak.realm=${REALM}
keycloak.resource=${CLIENT}
keycloak.credentials.secret=${CONFIDENTIAL}
```
- keycloak.auth-server-url : Keycloak's authentication url
- keycloak.realm : Realm name
- keycloak.resource : Client name
- keycloak.credentials.secret : Confidential code for the client

### Database settings

Create database with ddl/jwt-server.ddl and a user can access the database.
Change bellows for your DBMS.

```
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/gfarm
spring.datasource.username=${USERNAME}
spring.datasource.password=${PASSWORD}
```

- spring.datasource.url : URL for the database (ie. jdbc:mysql://localhost:3306/gfarm)
- spring.datasource.username : username of access user
- spring.datasource.password : password for the user

### Other settings (optional)

Optionaly add the bellow for Testing.

```
jwt-server.passphrase=${PASSPHRASE}
```

 - jwt-server.passphrase : Initial passphrase

You can get the passphrase using the secret API like the bellow.
```
curl -X POST -d "user=user1" -d "password=PASSWORD" -d ${PASSPHRAE} https://${HOST}/jwt-server/init_jwt
```
## Build

Build target/jwt-server.war with the following command.

```
mvn package
```

# Setup

Deploy jwt-server.war on the web application server like tomcat.