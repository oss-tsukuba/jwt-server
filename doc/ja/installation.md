# JWT Server インストール手順

コマンドプロンプトが「#」の場合はroot権限、「$」の場合は一般ユーザ権限での実行を意味する。
また、IdPサーバとしてKeycloakの利用を前提とする。Keycloak側の設定は [keycloak.md](keycloak.md) を参照する。

## OS

Rocky Linux 9を前提とする。

## インストール済パッケージの更新

インストールされているパッケージを更新する。
```
# dnf -y update
```

## JWT Serverソースコードの取得

### gitパッケージのインストール

```
# dnf -y install git
```

### ソースコード取得

```
$ git clone https://github.com/oss-tsukuba/jwt-server.git
```

## JDKのインストールと設定

JDK17以上が必要なのでOpenJDK21のパッケージをインストールし、Java環境を設定する。

```
# dnf -y install java-21-openjdk-devel
# alternatives --set java java-21-openjdk.x86_64
# alternatives --set javac java-21-openjdk.x86_64
```

## mariadbのインストールと設定

### mariadb パッケージのインストール

```
# dnf install -y mariadb-server
```

### 環境設定

```
# vi /etc/my.cnf.d/charset.cnf
```

（新規作成）

```
[mysqld]
character-set-server=utf8mb4
skip-character-set-client-handshake

[client]
default-character-set = utf8mb4
```

### mariadbの起動と自動起動設定

```
# systemctl enable mariadb
# systemctl start mariadb
```

### ユーザ＆データベースの作成

JWT Serverからアクセスするためのユーザを作成する。仮に、ユーザ名をjwtserver、パスワードをDBPASSWORD、データベース名をjwtserverdbとする。
```
# mysql -u root
MariaDB [(none)]> CREATE USER 'jwtserver'@'localhost' IDENTIFIED BY 'DBPASSWORD';
MariaDB [(none)]> CREATE DATABASE jwtserverdb;
MariaDB [(none)]> GRANT ALL PRIVILEGES ON jwtserverdb.* TO 'jwtserver'@'localhost';
MariaDB [(none)]> FLUSH PRIVILEGES;
MariaDB [(none)]> quit
```

### テーブル作成

```
$ mysql -u jwtserver -p jwtserverdb < jwt-server/ddl/jwt-server.ddl
Enter password: DBPASSWORD …データベース・ユーザーのパスワード
```

## tomcat のインストールと設定

### tomcatのインストール

tomcat10以上が必要なのでパッケージが存在しなければ下記の手順にてインストールする。
パッケージが存在すればパッケージを利用すればよい。

tomcat10をダウンロードして解凍する。
今後の運用のためにシンボリックリンクも作成しておく。
本書ではバージョン10.1.31を利用しているが新しいバージョンが
リリースされている場合にはバージョンを置き換えることで利用できる。

```
$ wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.31/bin/apache-tomcat-10.1.31.tar.gz
$ sudo tar -xf apache-tomcat-10.1.31.tar.gz -C /opt
$ sudo ln -s /opt/apache-tomcat-10.1.31 /opt/tomcat
```

tomcatの実行用ユーザtomcatを作成する。

```
# useradd -s /sbin/nologin tomcat
```

ただし、RHEL系Linuxでtomcatユーザ用に予約されているUIDとして53を利用する場合には下記のようにオプションをつけてユーザを作成する。

```
# useradd -u 53 -s /sbin/nologin tomcat
```

ファイルのオーナーをtomcatに変更する。

```
# chown -R tomcat: /opt/apache-tomcat-10.1.31
```

シェルスクリプトを実行可能にする。

```
# chmod +x /opt/tomcat/bin/*.sh
```

起動用のファイルを作成する。

```
# vi /etc/systemd/system/tomcat.service
```

（新規作成）
```
[Unit]
Description=Apache Tomcat 10 Web Application Server
After=network.target
[Service]
User=tomcat
Type=oneshot
PIDFile=/opt/tomcat/tomcat.pid
RemainAfterExit=yes

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

[Install]
WantedBy=multi-user.target
```

### apache httpdと連携するポートの設定

```
# vi /opt/tomcat/conf/server.xml
```
※ポート8080番の提供停止: 前後を「<!--」および「-->」で囲ってコメントにする

```
<!--
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
-->
```

※ポート8009番へのアクセスを提供: 前後の「<!--」および「-->」を削除し、下記のように変更する

```
<Connector protocol="AJP/1.3"
           address="localhost"
           port="8009"
           redirectPort="8443"
           secret="SECRET" />
```

「secret=」に続けてapache httpdからtomcatへの連携アクセスに用いるパスワードを設定する。本書では仮にSECRETとする。

### tomcatサービス登録および起動

```
# systemctl enable tomcat
# systemctl start tomcat
```

## JWT Serverのインストールと設定

### Maven のインストール

```
# dnf -y install maven
```

### JWT Serverの設定

```
$ vi jwt-server/src/main/resources/application.properties
```

Keycloakを利用する場合の設定例を示す。各値は利用環境に合わせて置き換える。

```properties
# OIDC settings (Keycloak example)
spring.security.oauth2.client.registration.oidc.client-id=CLIENT_ID
spring.security.oauth2.client.registration.oidc.client-secret=CLIENT_SECRET
spring.security.oauth2.client.registration.oidc.provider=oidc
spring.security.oauth2.client.registration.oidc.scope=openid
spring.security.oauth2.client.registration.oidc.authorization-grant-type=authorization_code
spring.security.oauth2.client.provider.oidc.issuer-uri=https://keycloak.example.org:443/auth/realms/REALM

# OAuth2 claim for user name
user-claim=userclaim

# contact address
contact-info=

# set redundant jwt-server's URLs
replicated-jwt-servers=

# MySQL settings
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/jwtserverdb …ホスト部にlocalhost、パス部にデータベース名を記載
spring.datasource.username=jwtserver …データベース・ユーザー名を記載
spring.datasource.password=DBPASSWORD …データベース・ユーザー用パスワードを記載
# don't change below
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database=MYSQL
spring.jpa.hibernate.ddl-auto=none

# spring settings
spring.main.allow-circular-references=true
```

OIDC対応前の設定から移行する場合は、`spring.security.oauth2.client.registration.keycloak.*` を `spring.security.oauth2.client.registration.oidc.*` に、`spring.security.oauth2.client.provider.keycloak.*` を `spring.security.oauth2.client.provider.oidc.*` に変更する。また、登録設定の `provider` の値を `keycloak` から `oidc` に変更する。Keycloakを利用する場合も、実装が参照する登録IDとプロバイダIDは `oidc` とする。

各設定値の内容は以下の通りである。

  - spring.security.oauth2.client.registration.oidc.client-id

    JWT ServerのクライアントIDを設定する。

  - spring.security.oauth2.client.registration.oidc.client-secret

    クライアントのシークレットを設定する。

  - spring.security.oauth2.client.registration.oidc.provider

    `oidc` を設定する。

  - spring.security.oauth2.client.registration.oidc.scope

    要求するScopeをカンマ区切りで設定する。`openid` を必ず含め、Keycloakやアプリケーションの設定に応じて必要なScopeを追加する。

  - spring.security.oauth2.client.registration.oidc.authorization-grant-type

    `authorization_code` を設定する。

  - spring.security.oauth2.client.provider.oidc.issuer-uri

    レルムを含めたKeycloakサーバのURLを設定する。Keycloakのバージョンや設定によってはパスに `/auth` を含まない（例: `https://keycloak.example.org/realms/REALM`）ので、利用環境に合わせる。

  - user-claim

    ユーザIDとして利用するトークンのクレームを設定する。

  - contact-info

    エラー画面に表示する管理者の連絡先等を設定する。必要ない場合は設定しなくてもよい。

  - replicated-jwt-servers

    JWT Serverを冗長化した場合に複数のJWT ServerのURLを空白区切りで設定する。設定すると冗長化したJWT Serverの利用方法が表示される。必要ない場合は設定しなくてもよい。

  - spring.datasource.url

    データベースのURLを設定する。MySQLを利用する想定である。

  - spring.datasource.username

    データベース・ユーザー名を設定する。

  - spring.datasource.password

    データベース・ユーザーのパスワードを設定する。

  - spring.datasource.driver-class-name

    ドライバのクラス名を設定する。変更不要である。

  - spring.jpa.database

    データベース（DBMS）の種類を設定する。変更不要である。

  - spring.jpa.hibernate.ddl-auto

    データベースのスキーマの生成方法を設定する。`none` のまま変更しないこと。`update` を設定すると、トークンを格納するカラムの型が変更され、トークンを保存できなくなる。

### JWT Serverのビルド

```
$ cd jwt-server
$ mvn package
```

### JWT Serverのデプロイ

```
$ sudo cp target/jwt-server.war /opt/tomcat/webapps/ROOT.war
```

## apache httpd のインストールと設定

### apache httpd から tomcat への連携設定

```
# vi /etc/httpd/conf.d/proxy-ajp.conf
```

（新規作成）

```
ProxyPass / ajp://localhost:8009/ secret=SECRET
ProxyPassReverse / ajp://localhost:8009/ secret=SECRET
```

「secret=」にはtomcatの `server.xml` で設定したパスワードと同じ値を設定する。値が一致しない場合、アクセスは403エラーとなる。

SELinuxの設定を変更し、tomcatへのネットワーク接続を許可する。

```
# setsebool -P httpd_can_network_connect 1
```

### apache httpd の自動起動設定および起動

```
# systemctl enable httpd
# systemctl start httpd
```

# JWT Serverの更新

JWT Serverのバージョンによってはデータベース・スキーマの変更手順が必要な可能性がありますので、リリースノートを必ず確認してください。

データベース・スキーマの変更が必要な場合は更新されたDDLをMySQL上で実行しデータベースを更新します。データベース・スキーマの変更がない場合、この手順は不要なので次の手順に進んでください。

```
$ mysql -u jwtserver -p jwtserverdb < jwt-server/ddl/jwt-server.ddl
Enter password: DBPASSWORD …データベース・ユーザーのパスワード
```

なお、DDLは `CREATE TABLE IF NOT EXISTS` で記述されているため、DDLの再実行では新規テーブルの追加のみが行われ、既存テーブルのカラム定義の変更は反映されません。既存テーブルの変更が必要な場合は、後述のバージョン毎の手順に従ってください。

「JWT Serverのビルド」の手順に従ってビルドし、正常にビルドできることを確認します。

apache httpd、tomcat の順で停止します。

```
# systemctl stop httpd
# systemctl stop tomcat
```

「JWT Serverのデプロイ」の手順に従い、デプロイを実施します。

tomcat、apache httpd の順で起動します。

```
# systemctl start tomcat
# systemctl start httpd
```

## 1.2.0 以前から 1.3.0 への更新

1.3.0 では設定プロパティ名、リダイレクトURI、データベース・スキーマが変更されています。上記の更新手順に加えて、以下の作業が必要です。

### 設定プロパティ名の変更

「JWT Serverの設定」に従い、ビルド前に `application.properties` の `spring.security.oauth2.client.registration.keycloak.*` と `spring.security.oauth2.client.provider.keycloak.*` を `oidc` に変更し、`provider` の値を `keycloak` から `oidc` に変更します。

### リダイレクトURIの変更

設定プロパティ名の変更にともない、JWT ServerのリダイレクトURIのパスが `/login/oauth2/code/keycloak` から `/login/oauth2/code/oidc` に変わります。Keycloakのクライアント設定の「Valid redirect URIs」に変更前のパスを登録している場合は、変更後のパスに更新してください。更新しないと、ログイン時にKeycloakで `Invalid parameter: redirect_uri` エラーとなります。

### カラム長の変更

`errors`、`issues`、`tokens`、`token_time` テーブルの `user`、および `tokens` テーブルの `audience`、`iv` のカラム長が varchar(255) に拡張されています。apache httpd と tomcat を停止した後、デプロイの前に以下を実施します。

データベースのバックアップを取得します。

```
$ mysqldump -u jwtserver -p jwtserverdb > jwtserverdb-backup.sql
Enter password: DBPASSWORD …データベース・ユーザーのパスワード
```

カラム長を変更します。`MODIFY` はカラム定義全体を置き換えるため、`NOT NULL` を省略しないでください。

```
$ mysql -u jwtserver -p jwtserverdb
Enter password: DBPASSWORD …データベース・ユーザーのパスワード
MariaDB [jwtserverdb]> ALTER TABLE `errors`
    ->   MODIFY `user` varchar(255) NOT NULL;
MariaDB [jwtserverdb]> ALTER TABLE `issues`
    ->   MODIFY `user` varchar(255) NOT NULL;
MariaDB [jwtserverdb]> ALTER TABLE `tokens`
    ->   MODIFY `user` varchar(255) NOT NULL,
    ->   MODIFY `audience` varchar(255) NOT NULL,
    ->   MODIFY `iv` varchar(255) NOT NULL;
MariaDB [jwtserverdb]> ALTER TABLE `token_time`
    ->   MODIFY `user` varchar(255) NOT NULL;
```

変更結果を確認します。

```
MariaDB [jwtserverdb]> SHOW CREATE TABLE `errors`;
MariaDB [jwtserverdb]> SHOW CREATE TABLE `issues`;
MariaDB [jwtserverdb]> SHOW CREATE TABLE `tokens`;
MariaDB [jwtserverdb]> SHOW CREATE TABLE `token_time`;
MariaDB [jwtserverdb]> quit
```

冗長構成の場合は、すべてのサーバで上記の `ALTER TABLE` を実行してください。[redundancy.md](redundancy.md) の設定でレプリケーションされるのは一部のテーブルのみで、`errors`、`issues` テーブルの変更は他のサーバに反映されません。レプリケーションされるテーブルについても、他のサーバで同じ `ALTER TABLE` を再度実行して問題ありません。

`ALTER TABLE` が `Specified key was too long` で失敗する場合は、InnoDBの行フォーマットが COMPACT または REDUNDANT になっている可能性があります。`ALTER TABLE tokens ROW_FORMAT=DYNAMIC;` で行フォーマットを変更してから再度実行してください。

# ユーザ毎にScopeを変更する場合

ユーザ毎にScopeを変更する場合は [keycloak.md](keycloak.md) を参照しユーザ毎にScopeを変更する設定を行ってください。さらにJWT Serverでは、下記のプロパティを設定してください。

```properties
spring.security.oauth2.client.registration.oidc.scope=openid,scope-1,scope-2
```

scope-1 および scope-2 はユーザ毎に変更させたいScopeです。すべてのScopeをここに記述してください。ここに記述するScopeは、すべてKeycloakでクライアントに関連付けておく必要があります。関連付けられていないScopeを記述すると、ログイン時に `invalid_scope` エラーとなります。
