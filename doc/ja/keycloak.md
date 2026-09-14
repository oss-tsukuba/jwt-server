# Keycloak 設定手順

本書ではJWT ServerでKeycloakを利用するさいの最低限の設定について説明する。なお、Keycloakはすでにインストールされており、利用可能な状態であることを前提とする。

管理者ユーザでログインし設定を行う。

## レルムの作成

「Realmn Setting」メニューを選択し、レルム「hpci」を作成する。

## クライアントの作成

レルム「hpci」配下に、クライアント「hpci-jwt-server」を作成する。「Access Type」はconfidentialとすることが望ましい。

## ユーザの作成

「Users」メニューより、必要に応じてユーザを作成する。

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
