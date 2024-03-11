# JWT Server の冗長化設定

それぞれのJWT Serverが利用するMariaDB Serverをマルチソースレプリケーション化する
ことでJWT Serverを冗長構成で稼働させることができる。

## 本書前提

* JWT Serverが利用するMariaDB Serverの設定ができている（JWT Serverのインストールマニュアル参照）
* データベース名はgfarmdbとする
* gfarmdbにアクセスするユーザはgfarmとする
* ２台のJWT Serverでの冗長構成とし、それぞれが利用するMariaDB ServerをServer1、Server2と呼ぶ

## Server1の設定

### 設定ファイルの変更

つぎの内容のファイルを作成し、/etc/my.cnf.d 配下に配置する。仮にmysql.cnfとする。

```
[mysqld]
log-bin
server-id=1
log-basename=master1

auto_increment_increment=5
auto_increment_offset=1

replicate-do-table=gfarmdb.tokens
replicate-ignore-table=gfarmdb.errors

master_retry_count=0
```

- log-bin: バイナリログを有効化する。
- server-id: ユニークなIDを指定する。
- log-basename: ログのファイル名を指定する。
- auto_increment_increment, auto_increment_offset: Server2と重複しないように連続するカラム値の間隔を指定する。
- replicate-do-table: レプリケーションを実行するテーブルを指定する。
- replicate-ignore-table: レプリケーションを実行しないテーブルを指定する。
- master_retry_count: マスタに接続できない場合に接続を試みる回数を指定する（0:無限）。

  サーバを再起動する。

```
# systemctl restart mysqld
```

### マスタ/スレーブの設定

ユーザgfarmでgfarmdbに接続し下記を実行する。

レプリケーション用のユーザ（replica）を作成し権限を設定する。パスワード（PASSWORD）はServer2の設定で利用する。
```
CREATE USER 'replica'@'%' IDENTIFIED BY 'PASSWORD';
RESET MASTER;
GRANT REPLICATION SLAVE ON *.* TO 'replica'@'%';
```

スレーブの設定を行う。
```
CHANGE MASTER TO
 MASTER_HOST = 'Server2',
 MASTER_PORT = 3306,
 MASTER_USER = 'replica',
 MASTER_PASSWORD = 'PASSWORD',
 MASTER_CONNECT_RETRY = 10,
 MASTER_USE_GTID = slave_pos;
START SLAVE;
```

- MASTER_HOST: マスタのホストを設定する。ここではServer2を指定する。
- MASTER_PORT: Server2のポート番号を指定する。
- MASTER_USER: レプリケーション用にServer2で作成したユーザを指定する。
- MASTER_PASSWORD: Server2で指定したパスワードを指定する。
- MASTER_CONNECT_RETRY: 接続を試行する間隔を指定する（デフォルトは60秒）
- MASTER_USE_GTID: マスタのGTIDを指定する。

## Server2の設定

### 設定ファイルの変更

つぎの内容のファイルを作成し、/etc/my.cnf.d 配下に配置する。仮にmysql.cnfとする。

```
[mysqld]
log-bin
server-id=2
log-basename=master2

auto_increment_increment=5
auto_increment_offset=2

replicate-do-table=gfarmdb.tokens
replicate-ignore-table=gfarmdb.errors,gfarmdb.issues

master_retry_count=0
```

- log-bin: バイナリログを有効化する。
- server-id: ユニークなIDを指定する。
- log-basename: ログのファイル名を指定する。
- auto_increment_increment, auto_increment_offset: Server1と重複しないように連続するカラム値の間隔を指定する。
- replicate-do-table: レプリケーションを実行するテーブルを指定する。
- replicate-ignore-table: レプリケーションを実行しないテーブルを指定する。
- master_retry_count: マスタに接続できない場合に接続を試みる回数を指定する（0:無限）。

  サーバを再起動する。

```
# systemctl restart mysqld
```

### マスタ/スレーブの設定

ユーザgfarmでgfarmdbに接続し下記を実行する。

レプリケーション用のユーザ（replica）を作成し権限を設定する。パスワード（PASSWORD）はServer1の設定で利用する。
```
CREATE USER 'replica'@'%' IDENTIFIED BY 'PASSWORD';
RESET MASTER;
GRANT REPLICATION SLAVE ON *.* TO 'replica'@'%';
```

スレーブの設定を行う。
```
CHANGE MASTER TO
 MASTER_HOST = 'Server1',
 MASTER_PORT = 3306,
 MASTER_USER = 'replica',
 MASTER_PASSWORD = 'PASSWORD',
 MASTER_CONNECT_RETRY = 10,
 MASTER_USE_GTID = slave_pos;
START SLAVE;
```

- MASTER_HOST: マスタのホストを設定する。ここではServer1を指定する。
- MASTER_PORT: Server1のポート番号を指定する。
- MASTER_USER: レプリケーション用にServer1で作成したユーザを指定する。
- MASTER_PASSWORD: Server1で指定したパスワードを指定する。
- MASTER_CONNECT_RETRY: 接続を試行する間隔を指定する（デフォルトは60秒）
- MASTER_USE_GTID: マスタのGTIDを指定する。

## 運用時の障害について

### サーバダウン時

一方のサーバがダウンし復旧した場合、何もせずとも同期が再開し正常に稼働する。

### Server1、Server2のネットワーク障害時

Server1、Server2間のネットワークに障害が発生し、その間にServer1、およびServer2の両者にて
パスフレーズの変更が行われた場合、ネットワーク障害解消後にサーバ間の状態に齟齬が生じる。
この状態を解消させるにはどちらかのサーバにてパスフレーズの変更を行う必要がある。
また、トークンの取得が行われた場合も齟齬は生じるが、トークンの再取得にてその状態は
解消される。
