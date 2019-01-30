# MLDPTerminal

Microsoft社開発のMLDPを使用したターミナルエミュレータです．

MLDPに対応したBLEモジュール(RN4020等)を搭載したデバイスをFreeBSDにUSB接続することで,androidからCUI操作できることを確認．
android4.0以上で動作確認済み

画面上側に配置してあるSCANボタンで周りに存在するBLE機器をスキャンできます.
このターミナルエミュレータでは，こちらのサイト(https://www.mm2d.net/main/prog/c/console-02.html )に記載してあるエスケープシーケンスを実装しています
（ESC[nmに関しては30~37, 39のみ）

iOS版
https://github.com/i14tumori/MLDP-Terminal

使い方も同様
