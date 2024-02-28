Release note for JWT server 1.0.0
=================================

[2024.2.29]

JWT server is a web application that stores refresh tokens of OAuth2
securely, and provides the access token using
[jwt-agent](https://github.com/oss-tsukuba/jwt-agent.git) and
[jwt-logon](https://github.com/oss-tsukuba/jwt-logon.git).  Tht JWT
server assumes to use Keycloak as an identity provider.

When you log in the JWT server, access information will be provided,
which includes a user name and passphrase to retrieve an access token
by `jwt-agent` or `jwt-logon`.

JWT servers can be redundant using DBMS multi-source replication(see
).  Also, `jwt-agent` and `jwt-logon` can specify multiple redundant
JWT servers.  If a server is down, they cloud retriave an access token
from other working servers.
