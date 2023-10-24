# Keycloak 設定手順

本書ではJWT ServerでKeycloakを利用するさいの最低限の設定について説明する。なお、Keycloakはすでにインストールされており、利用可能な状態であることを前提とする。

管理者ユーザでログインし設定を行う。

## レルムの作成

「Realmn Setting」メニューを選択し、レルム「hpci」を作成する。

## クライアントの作成

レルム「hpci」配下に、クライアント「hpci-jwt-server」を作成する。「Access Type」はconfidentialとすることが望ましい。

## ユーザの作成

「Users」メニューより、必要に応じてユーザを作成する。
