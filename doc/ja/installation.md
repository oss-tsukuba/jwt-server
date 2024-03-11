# JWT Server インストール手順

コマンドプロンプトが「#」の場合はroot権限、「$」の場合は一般ユーザ権限での実行を意味する。
また、IdPサーバとしてKeycloakの利用を前提とする。

## OS

Rocky Linux 9を前提とする。

## インストール済パッケージの更新

インストールされているパッケージを更新する。
```
# dnf -y update
```

## jwt-serverソースコードの取得

### gitパッケージのインストール

```
# dnf -y install git
```

### ソースコード取得

```
$ git clone https://github.com/oss-tsukuba/jwt-server.git
```

## mariadbのインストールと設定

### mariadb パッケージのインストール

```
# dnf install -y mariadb mariadb-server
```

### mysqlの起動と自動起動設定

```
# systemctl enable mariadb
# systemctl start mariadb
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

### mysqlの再起動

```
# systemctl restart mariadb
```

### ユーザ＆データベースの作成

jwt-serverからアクセスするためのユーザを作成する。仮に、ユーザ名をjwtserver、パスワードをDBPASSWORD、データベース名をjwtserverdbとする。
```
$ mysql -u root
MariaDB [(none)]> CREATE USER 'jwtserver'@'localhost' IDENTIFIED BY ‘DBPASSWORD';
MariaDB [(none)]> CREATE DATABASE jwtserverdb;
MariaDB [(none)]> GRANT ALL PRIVILEGES ON jwtserverdb . \* TO 'jwtserver'@'localhost';
MariaDB [(none)]> FLUSH PRIVILEGES;
MariaDB [(none)]> quit
```

### テーブル作成

```
$ mysql -u jwtserver -p jwtserverdb < jwt-server/ddl/jwt-server.ddl
Enter password: DBPASSWORD …データベース・ユーザーのパスワード
```

## tomcat のインストールと設定

### tomcatパッケージのインストール

```
# dnf -y install tomcat
```

### apache httpdと連携するポートの設定

```
# vi /usr/share/tomcat/conf/server.xml
```
※ポート8080番の提供停止: 前後を「<!--」および「-->」で囲ってコメントにする

```
<!--
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
-->
```

※ポート8009番へのアクセスを提供: 前後の「<!--」および「-->」を削除

```
<Connector protocol="AJP/1.3"
           address="localhost"
           port="8009"
           redirectPort="8443"
           secret="SECRET" />
```
「secret=」に続けてapache httpdからtomcatへの連携アクセスに用いるパスワードを設定する。

### tomcatサービス登録および起動

```
# systemctl enable tomcat
# systemctl start tomcat
```

## jwt-server のインストールと設定

### Maven のインストール

```
# dnf -y install maven
```

### jwt-serverの設定

```
% vi jwt-server/src/main/resources/application.properties
```

（設定例)
```
# change keycloak settings
keycloak.enabled=true
keycloak.auth-server-url=https://keycloak.example.org:443/auth …KeycloakサーバーのURLを記載
keycloak.realm=realm
keycloak.resource=clientId
keycloak.public-client=false
keycloak.credentials.secret=KEYCLOAK_SECRET
user-claim=hoge.id

# MySQL settings
spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:3306/jwtserverdb …ホスト部にlocalhost、パス部にデータベース名を記載
spring.datasource.username=jwtserver …データベース・ユーザー名を記載
spring.datasource.password=DBPASSWORD …データベース・ユーザー用パスワードを記載
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database=MYSQL
spring.jpa.hibernate.ddl-auto=update

# others
contact-info=
replicated-jwt-servers=
```

各設定値の内容は以下の通りである。

  - keycloak.enabled

    Keycloak Spring Boot アダプターの有効/無効を設定する。trueを設定する。

  - keycloak.auth-server-url

    KeycloakサーバのURLを設定する。

  - keycloak.realm

    レルムを設定する。

  - keycloak.resource

    jwt-serverのクライアントIDを設定する。

  - keycloak.public-client

    クライアントがpublicか否かを設定する。HPCI環境におけるjwt-serverはconfidentialクライアントとして運用するため、falseを設定する

  - keycloak.credentials.secret

    クライアントのシークレットを設定する。

  - user-claim

    ユーザIDとして利用するトークンのクレームを設定する。

  - spring.datasource.url

    データベースのURLを設定する。MySQLを利用する想定である。

  - spring.datasource.username

    データベース・ユーザー名を設定する。

  - spring.datasource.password

    データベース・ユーザーのパスワードを設定する。

  - spring.datasource.driver-class-name

    ユーザのパスワードを設定する。

  - spring.jpa.database

    データベース（DBMS）の種類を設定する。

  - spring.jpa.hivernate.ddl-auto

    データベースのスキーマの生成方法を設定する。

  - contact-info

    エラー画面に表示する管理者の連絡先等を設定する。必要ない場合は設定しなくてもよい。

  - replicated-jwt-servers

    JWT Serverを冗長化して場合に複数のJWT ServerのURLを空白区切りで設定する。設定すると冗長化したJWT Serverの利用方法が表示される。必要ない場合は設定しなくてもよい。

### jwt-serverのビルド

```
$ cd jwt-server
$ mvn package
```

### jwt-serverのデプロイ

```
$ sudo cp target/jwt-server.war /usr/share/tomcat/webapps/ROOT.war
```

## apache httpd のインストールと設定

### apache httpd から tomcat への連携設定

```
# vi /etc/httpd/conf.d/proxy-ajp.conf
```

(新規作成)

```
ProxyPass / ajp://localhost:8009/ secret=TQJvCWhkNjULELwF
ProxyPassReverse / ajp://localhost:8009/ secret=TQJvCWhkNjULELwF
```

SELinuxの設定を変更し、tomcatへのネットワーク接続を許可する。

```
# setsebool -P httpd\_can\_network\_connect 1
```

### apache httpd の自動起動設定および起動

```
# systemctl enable httpd
# systemctl start httpd
```
