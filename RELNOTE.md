Release note for JWT server 1.3.0
=================================

[2026.11.25]

### New features
* support OpenID Connect (OIDC) identity providers
* support opaque access tokens

### Updated features
* support token refresh with or without refresh token rotation
* display an error message when unauthorized

### Upgrade notes
* rename `spring.security.oauth2.client.registration.keycloak.*` and
  `spring.security.oauth2.client.provider.keycloak.*` to `...oidc.*`,
  and change the `provider` value from `keycloak` to `oidc`
* change the valid redirect URI of the client on the identity provider
  from `/login/oauth2/code/keycloak` to `/login/oauth2/code/oidc`
* extend the column lengths of the existing tables: `user`, `audience`
  and `iv` to varchar(255) (see README.md for the procedure)

Release note for JWT server 1.2.0
=================================

[2025.3.10]

### Updated features
* display the date of issue when logged in if already issued
* change "Date of Issue" in the advanced menu to show only the most recent log

Release note for JWT server 1.1.0
=================================

[2024.12.16]

### New feature
* use "Spring OAuth2 Client" instead of "Keycloak Spring Security Adapter"

### Updated feature
* record token issues

Release note for JWT server 1.0.1
=================================

[2024.4.12]

### New application properties
* replicated-jwt-servers - specifies replicated JWT servers for displaying jwt-agent usage.

Release note for JWT server 1.0.0
=================================

[2024.2.29]

JWT server is a web application that stores refresh tokens of OAuth2 securely, and provides the access token using [jwt-agent](https://github.com/oss-tsukuba/jwt-agent.git) and [jwt-logon](https://github.com/oss-tsukuba/jwt-logon.git).  The JWT server assumes the use of Keycloak as an identity provider.

When you log in to the JWT server, access information will be provided, which includes a user name and passphrase to retrieve an access token by `jwt-agent` or `jwt-logon`.

JWT servers can be redundant using DBMS multi-source replication.  When `jwt-agent` or `jwt-logon` specifies multiple redundant JWT servers, an active JWT server is automatically selected.
