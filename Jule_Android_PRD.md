# Android版 インターネットTVアプリ要件定義書（PRD）

## プロジェクト概要
Windows向けインターネットTVプレーヤー「DokodemoTV (DragonVideoPlayerベース)」の主要機能・再生ロジックを抽出し、AndroidモバイルおよびTV端末向けのネイティブアプリとして開発します。
開発エージェント "Jule" は本ドキュメントをベースにAndroidアプリの初期構築から詳細実装までを行ってください。

## 開発環境・技術スタック要件
- 言語: Kotlin
- UIフレームワーク: Jetpack Compose (Android TV対応を考慮してCompose for TVも検討)
- アーキテクチャ: MVVM
- 再生エンジン: Media3 (ExoPlayer) - HLS(m3u8)再生用
- デザインパターン: Material Design 3

## 実装要件・機能一覧

### 1. ストリーミング再生機能 (コア要素)
- 起動時に `assets` フォルダ内に保存されたテキストファイル（`abema.txt`, `primehome.txt`, `radiko.txt` 等）の各行（m3u8 URL等）を読み込み、チャンネルリストとして保持する。
- Media3 を用いて対象URLを再生(HLSサポートを有効化)する。
- 画面上のタップやスワイプ操作で、チャンネル（URL）の切り替えを行えるようにする。

### 2. 録画機能の対応策
- 再生中のHLSストリームを手動で録画開始・停止する機能。
- ダウンロード機能として `DownloadManager` などを利用し、端末のパブリックストレージ（Movies等）へ動画ファイルとして保存する機能。

### 3. テレビ欄 (EPG) UI機能
- チャンネル一覧とは別に「番組表」画面（GridViewやLazyVerticalGridを用いる）を用意する。
- （現在はモックデータで可）放送時間、チャンネル、番組名からなる番組表グリッドを表示し、タップで対象チャンネルの再生を開始できるようにする。

### 4. 設定・カスタム機能
- 視聴履歴機能（RoomによるSQLiteデータベース保存）。
- オーディオ・ビデオエフェクト設定（ExoPlayerのProcessor等を利用したイコライザー実装）。

## Juleへの依頼手順（プロンプト用）
Juleはこの文書を読み込み、以下の手順で開発を進めてください：
1. `Android Studio` またはCLIで新規プロジェクト（Jetpack Compose）を作成。
2. Media3の依存関係 (`androidx.media3:media3-exoplayer:1.1.1` 等) を `build.gradle` に追加。
3. `assets` からURLリストを読み込むViewModelとRepositoryの作成。
4. ExoPlayerをラップしたComposeの再生画面UI構築と、テレビ欄画面UIの構築。
