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

## JDKのインストールと設定

OpenJDK21のパッケージをインストールし、Java環境を設定する。

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

### mysqlの起動と自動起動設定

```
# systemctl enable mariadb
# systemctl start mariadb
```

### ユーザ＆データベースの作成

jwt-serverからアクセスするためのユーザを作成する。仮に、ユーザ名をjwtserver、パスワードをDBPASSWORD、データベース名をjwtserverdbとする。
```
$ mysql -u root
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

tomcat10をダウンロードして解凍する。
今後の運用のためにシンボリックリンクも作成しておく。
本書ではバージョン10.1.31を利用しているが新しいバージョンが
リリースされている場合にはバージョンを置き換えることで利用できる。

```
% wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.31/bin/apache-tomcat-10.1.31.tar.gz
% sudo tar -xf apache-tomcat-10.1.31.tar.gz -C /opt
% sudo ln -s /opt/apache-tomcat-10.1.31 /opt/tomcat
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
ExecReStart=/opt/tomcat/bin/shutdown.sh;/opt/tomcat/bin/startup.sh

[Install]
WantedBy=multi-user.target
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
# Keycloak settings
spring.security.oauth2.client.registration.keycloak.client-id=CLIENT_ID … クライアントIDを記載
spring.security.oauth2.client.registration.keycloak.client-secret=KEYCLOAK_SECRET … シークレットを記載
spring.security.oauth2.client.registration.keycloak.provider=keycloak
spring.security.oauth2.client.registration.keycloak.scope=openid
spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://keycloak.example.org:443/auth/realms/REALM … REALMを含めたKeycloakサーバーのURLを記載


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
spring.jpa.hibernate.ddl-auto=update

# spring settings
spring.main.allow-circular-references=true
```

各設定値の内容は以下の通りである。

  - spring.security.oauth2.client.registration.keycloak.client-id

    jwt-serverのクライアントIDを設定する。

  - spring.security.oauth2.client.registration.keycloak.client-secret

    クライアントのシークレットを設定する。

 - spring.security.oauth2.client.provider.keycloak.issuer-uri

    レルムを含めたKeycloakサーバのURLを設定する。

  - user-claim

    ユーザIDとして利用するトークンのクレームを設定する。

  - contact-info

    エラー画面に表示する管理者の連絡先等を設定する。必要ない場合は設定しなくてもよい。

  - replicated-jwt-servers

    JWT Serverを冗長化して場合に複数のJWT ServerのURLを空白区切りで設定する。設定すると冗長化したJWT Serverの利用方法が表示される。必要ない場合は設定しなくてもよい。

  - spring.datasource.url

    データベースのURLを設定する。MySQLを利用する想定である。

  - spring.datasource.username

    データベース・ユーザー名を設定する。

  - spring.datasource.password

    データベース・ユーザーのパスワードを設定する。

  - spring.datasource.driver-class-name

    ドライバのクラス名を設定する。変更不要です。

  - spring.jpa.database

    データベース（DBMS）の種類を設定する。変更不要です。

  - spring.jpa.hivernate.ddl-auto

    データベースのスキーマの生成方法を設定する。変更不要です。

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
# setsebool -P httpd_can_network_connect 1
```

### apache httpd の自動起動設定および起動

```
# systemctl enable httpd
# systemctl start httpd
```
