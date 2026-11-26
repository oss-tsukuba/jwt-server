# Keycloak 設定手順

本書ではJWT ServerでKeycloakを利用する際の最低限の設定について説明する。なお、Keycloakはすでにインストールされており、利用可能な状態であることを前提とする。

管理者ユーザでログインし設定を行う。

## レルムの作成

「Create realm」より、レルム「hpci」を作成する。

## クライアントの作成

レルム「hpci」配下に、クライアント「hpci-jwt-server」を作成する。「Client authentication」をONとし、confidentialクライアントとすることが望ましい（古いバージョンのKeycloakでは「Access Type」をconfidentialとする）。

「Valid redirect URIs」には、JWT ServerのリダイレクトURIを設定する。JWT ServerのURLが `https://jwt-server.example.org/` の場合は `https://jwt-server.example.org/login/oauth2/code/oidc` となる。設定しない場合、ログイン時に `Invalid parameter: redirect_uri` エラーとなる。

「Valid post logout redirect URIs」には、JWT ServerのURLを設定する。JWT ServerのURLが `https://jwt-server.example.org/` の場合は `https://jwt-server.example.org` となる。設定しない場合は「Valid redirect URIs」の値が使われるが、上記のリダイレクトURIとは一致しないため、「IdP Logout」の実行時にKeycloakで `Invalid redirect uri` エラーとなり、Keycloakからログアウトされない。

## ユーザの作成

「Users」メニューより、必要に応じてユーザを作成する。

## リフレッシュトークンのローテーションを有効にする場合

レルムの「Realm settings」→「Tokens」で「Revoke Refresh Token」を有効にすると、リフレッシュトークンは一度使うと無効になり、アクセストークンの取得のたびに新しいリフレッシュトークンが発行される。JWT Serverは新しいリフレッシュトークンを保存するので、この設定でも利用できる。

ただし、Keycloakは使用済みのリフレッシュトークンが再度使われたことを検知すると、そのログインで発行したトークンをすべて無効にする。この場合、JWT Serverに保存されているトークンも使えなくなり、jwt-agentやjwt-logonは認証エラーとなる。以下の場合にこの状態になるので注意する。

- jwt-agentやjwt-logonでアクセストークンを取得した後に、同じブラウザのセッションでトークンを再生成した場合。「Possibly Unauthorized」と表示され、再生成に失敗する。再生成する場合は、先に「IdP Logout」でログアウトし、ログインし直す。
- 同じパスフレーズを使って、複数のホストから同時にアクセストークンを取得した場合。
- JWT Serverを冗長化していて、データベースのレプリケーションが遅延または停止している間に、複数のJWT Serverでアクセストークンを取得した場合。

この状態になった場合は、「IdP Logout」でログアウトしてログインし直し、トークンを再生成する。

## ユーザ毎にトークンのScopeを変更する場合

Role毎にScopeを設定し、各ユーザにRoleを設定する。
Role:role-1に対してScope:scope-1を設定し、User:user1に設定する例をもとに説明する。

1.  **Realm Role を作成**

    -   `Realm roles` → `Create role`
    -   `role-1` を作成

2.  **Client Scope を作成**

    -   `Client scopes` → `Create client scope`
    -   `scope-1` を作成
    -   Protocol: `OpenID Connect`

3.  **Client Scope と Role を関連付け**

    -   `Client scopes` → `scope-1` → `Scope`
    -   `Assign role` で `role-1` を割り当てる

4.  **Include in token scope を ON にする【重要】**

    -   `Client scopes` → `scope-1` → `Settings`
    -   `Include in token scope` → **ON**

    OFF の場合、Role の判定が正しくても `scope-1` がアクセストークンの
    `scope` claim に含まれない。

5.  **Client に Client Scope を Optional で関連付け**

    -   `Clients` → 対象 Client → `Client scopes`
    -   `scope-1` を追加
    -   Assigned type: **Optional**

6.  **User と Role を関連付け**

    -   `Users` → `user1` → `Role mapping` → `Assign role`
    -   `role-1` を割り当てる
