# MLDPTerminal

Microchip社開発のMLDPを使用したターミナルエミュレータ

MLDPに対応したBLE(RN4020等)を搭載したデバイスをFreeBSDにUSB接続することでandroidからCUI操作可能

## エスケープシーケンス
[こちらのサイト](https://www.mm2d.net/main/prog/c/console-02.html )に記載してあるエスケープシーケンスを実装

（ESC[nmに関しては30~37, 39のみ）

## 動作環境
android7.0以上

## iOS版
https://github.com/i14tumori/MLDP-Terminal
