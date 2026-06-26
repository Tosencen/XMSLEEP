<h1 align="center"> 📱 XMSLEEP
 </h1>

<div align="center">

ホワイトノイズと自然音を再生する Android アプリ。リラックス、集中、睡眠をサポートします。

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://www.android.com/)

[ダウンロード](#ダウンロード) • [機能](#機能) • [使い方](#使い方)

**Language**: [中文](README.md) | [繁體中文](README_ZH_TW.md) | [English](README_EN.md) | [한국어](README_KO.md) | [Русский](README_RU.md) | 日本語

<a href="https://hellogithub.com/repository/Tosencen/XMSLEEP" target="_blank"><img src="https://abroad.hellogithub.com/v1/widgets/recommend.svg?rid=3cbf370c9f534ea3bf3695b7f9b8bd19&claim_uid=Gxvd2eIyHm54S9p" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>
</div>

## 📱 スクリーンショット

<div align="center">

<table>
  <tr>
    <td align="center">
      <a href="screenshots/1.jpg"><img src="screenshots/1.jpg" alt="スクリーンショット1" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/2.jpg"><img src="screenshots/2.jpg" alt="スクリーンショット2" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/3.jpg"><img src="screenshots/3.jpg" alt="スクリーンショット3" width="200"/></a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="screenshots/4.jpg"><img src="screenshots/4.jpg" alt="スクリーンショット4" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/5.jpg"><img src="screenshots/5.jpg" alt="スクリーンショット5" width="200"/></a>
    </td>
    <td align="center">
      <a href="screenshots/6.jpg"><img src="screenshots/6.jpg" alt="スクリーンショット6" width="200"/></a>
    </td>
  </tr>
</table>

</div>

---

## 📱 概要

XMSLEEP はホワイトノイズと自然音の再生に特化した Android アプリです。雨音、焚き火、鳥のさえずりなど、厳選された高品質な自然音でリラックス、集中力向上、睡眠の質向上をサポートします。Material Design 3 に準拠した、クリーンで美しいインターフェースを備えています。

## ✨ 機能

### 🎵 オーディオ機能
- **多彩なホワイトノイズ**: 雨、焚き火、雷、鳥のさえずり、虫の音など、多数の自然音
- **オンラインオーディオ**: GitHub から動的にオーディオリソースを追加可能
- **ローカルオーディオ**: 端末内のオーディオファイル再生に対応
- **シームレスループ**: 途切れないループ再生で没入感を提供
- **音量コントロール**: サウンドごとに個別音量調節、または一括調整
- **音量保存**: 設定はアプリ再起動後も自動保持
- **Bluetooth ヘッドセット対応**: 切断時に自動停止
- **Bilibili ラジオ**: Bilibili ライブ配信ルームを検索・再生、お気に入りルームをピン留め可能

### 🎨 インターフェース
- **美しいアニメーション**: 各サウンドに WebP アニメーションを搭載
- **Material Design 3**: 最新のデザインガイドラインを採用
- **テーマ切替**: ライト/ダークモード、システムテーマに自動追従
- **カスタムテーマ**: 多彩なカラーテーマとダイナミックカラー対応

### ⚙️ 便利機能
- **タイマー機能**: 自動停止時間を設定可能
- **プリセット**: 3 つのプリセット、各最大 10 個のサウンドを保存
- **お気に入り**: 好みのサウンドを保存
- **最近再生**: 再起動時に最近の再生リストを表示
- **フローティングボタン**: 再生中のサウンドを表示、一時停止・展開可能
- **スマート操作**: フローティングボタンとプリセットは排他的、スクロール時に自動格納
- **自動更新**: GitHub Releases によるアップデート確認

## 🛠️ 技術スタック

- **Kotlin** - メイン開発言語
- **Jetpack Compose** - モダン UI フレームワーク
- **Material Design 3** - UI デザインシステム
- **ExoPlayer/Media3** - オーディオ再生エンジン
- **OkHttp** - ネットワーク通信
- **Gson** - JSON パース
- **Kotlinx Serialization** - JSON シリアライズ
- **Coil** - 画像読み込み
- **WebP** - アニメーション
- **MaterialKolor** - 動的テーマカラー生成
- **Accompanist** - Pull-to-refresh
- **Hilt** - 依存性注入フレームワーク
- **Lottie** - 高品質アニメーションレンダリング

## 📦 現在のバージョン

- **バージョン**: 2.2.6
- **Version Code**: 41
- **最小 SDK**: Android 8.0 (API 26)
- **ターゲット SDK**: Android 15 (API 35)

### 🆕 最新アップデート (v2.2.6)

#### 🎨 新機能
- **Bilibili ラジオ**: Bilibili ライブ配信ルーム機能を追加。自習室、ホワイトノイズなどのカテゴリを検索可能
- **ピン留め**: よく使うルームをピン留めして素早く切替、配信していないルームはグレー表示

#### ✨ 改善
- **編集モード最適化**: ボトムシートを閉じる際にピン編集モードを自動的に解除
- **プリセットダイアログ改善**: プリセット名編集後にダイアログを閉じる際に編集状態をリセット

### 過去のバージョン

#### v2.2.3
- **一言ウィジェット**: ホーム画面に時間、日替わり名言、更新ボタンを表示するウィジェット追加

#### v2.2.1
- **呼吸エクササイズ**: 呼吸ガイド機能を追加
- **画面常時表示**: 画面点灯設定を最適化
- **天気マッピング**: 天気とオーディオのマッピングを改善

## 🚀 ダウンロード

最新版は [GitHub Releases](https://github.com/Tosencen/XMSLEEP/releases) からダウンロードできます。

## 📋 ビルド要件

- **Android Studio**: Ladybug | 2024.2.1 以上
- **JDK**: 17 以上
- **Android SDK**: API 33 以上
- **Gradle**: 8.0 以上

## 🔨 ビルド手順

1. **リポジトリをクローン**
   ```bash
   git clone https://github.com/Tosencen/XMSLEEP.git
   cd XMSLEEP
   ```

2. **Gradle 設定**
   - `gradle.properties.example` を `gradle.properties` にコピー
   - (任意) GitHub Token を設定

3. **プロジェクトを開く**
   - Android Studio で開く
   - Gradle 同期

4. **実行**
   - デバイスを接続またはエミュレーター起動
   - 実行ボタンをクリック

## 📖 使い方

### 基本操作
1. **再生**: サウンドカードをタップして再生、再タップで停止
2. **音量調整**: カード右下の音量アイコンから個別調整
3. **タイマー**: 右下のタイマーボタンから自動停止時間を設定

### 画面操作
4. **テーマ切替**: 左上のテーマボタンでライト/ダーク切替
5. **カスタム設定**: 設定画面でテーマカラー、アニメーション等を調整
6. **プリセット管理**: サウンドカードタイトルから「プリセットに追加」
7. **プリセット切替**: 下部プリセットエリアで 3 つのプリセットを切替
8. **お気に入り**: サウンドカードタイトルから「お気に入り」追加
9. **Bilibili ラジオ**: ラジオページの検索ボタンから Bilibili ライブルームをカテゴリ検索、ピンアイコンでよく使うルームを保存

### 高度な機能
10. **フローティングボタン**: 再生中に表示、タップで再生リスト確認
11. **一括追加**: フローティングボタン展開中にプリセットへ一括追加
12. **スマート格納**: スクロールやタブ切替時に自動格納

## ⚠️ サウンドソース

- **内蔵サウンド**: オープンソースオーディオライブラリ
- **オンラインサウンド**: [moodist](https://github.com/remvze/moodist) プロジェクト (MIT)
- **サードパーティ**: 各ライセンスに準拠
  - **Pixabay Content License**: [Pixabay](https://pixabay.com/service/license-summary/)
  - **CC0**: [Creative Commons Zero](https://creativecommons.org/publicdomain/zero/1.0/)

## 📄 ライセンス

このプロジェクトは [MIT License](LICENSE) の下で公開されています。

## 🤝 コントリビューション

Issue と Pull Request を歓迎します！

### コントリビューションガイド
1. リポジトリを Fork
2. 機能ブランチを作成 (`git checkout -b feature/AmazingFeature`)
3. 変更をコミット (`git commit -m 'Add some AmazingFeature'`)
4. ブランチにプッシュ (`git push origin feature/AmazingFeature`)
5. Pull Request を開く

## 👤 作成者

**Tosencen**

- GitHub: [@Tosencen](https://github.com/Tosencen)

## 🙏 謝辞

- [moodist](https://github.com/remvze/moodist) - オンラインオーディオリソース
- [Material Design 3](https://m3.material.io/) - UI デザインガイドライン
- [MaterialKolor](https://github.com/material-foundation/material-color-utilities) - 動的カラースキーム

---

<div align="center">

**⭐ このプロジェクトが役に立ったら Star をお願いします！**

© 2026 XMSLEEP. All rights reserved.

</div>
