Release note for JWT server 1.2.0
=================================

[2025.3.10]

### Updated feature
* display date of issue when logged in if alreadh issued
* change "Date of Issue" in the advanced menu to show only the most recent log

Release note for JWT server 1.1.0
=================================

[2024.12.16]

### New feature
* use "Spring Oauth2 Client" instead of "Keycloak Spring Security Adapter"

### Updatd feature
* record token issues

Release note for JWT server 1.0.1
=================================

[2024.4.12]

### New application properties
* replicated-jwt-servers - specifies replicated JWT servers for displaying jwt-agent usage.

Release note for JWT server 1.0.0
=================================

[2024.2.29]

JWT server is a web application that stores refresh tokens of OAuth2 securely, and provides the access token using [jwt-agent](https://github.com/oss-tsukuba/jwt-agent.git) and [jwt-logon](https://github.com/oss-tsukuba/jwt-logon.git).  Tht JWT server assumes to use Keycloak as an identity provider.

When you log in the JWT server, access information will be provided, which includes a user name and passphrase to retrieve an access token by `jwt-agent` or `jwt-logon`.

JWT servers can be redundant using DBMS multi-source replication.  When `jwt-agent` or `jwt-logon` specifies multiple redundant JWT servers, an active JWT server is automatically selected.
