# DokodemoTV Android版 アーキテクチャおよび技術設計書

Windows版「DragonVideoPlayer」から抽出した技術と、それをAndroid環境に落とし込むための設計方針を記載します。

## 1. 抽出されたコア技術とAndroid上での代替手段

### 1-1. ストリーミング再生エンジン
- **Windows版**: `mpv.exe` プロセスをC#から起動してパイプ通信で制御。
- **Android版 (`androidx.media3:media3-exoplayer`)**: Androidで標準的なMedia3 (ExoPlayer) を使用します。HLS (m3u8) 形式のストリーミング再生をネイティブでサポートしており、mpvのような外部プロセスの管理が不要で安定した動作が期待できます。

### 1-2. URL・プレイリスト解決・抽出
- **Windows版**: `yt-dlp.exe` または `streamlink.exe` を使用して、m3u8の生URLを抽出。
- **Android版**: 
  - `radiko.jp` や一部専用ストリーミングは直接HLS URLを持っているため、`abema.txt` 等のテキストリソースをAndroidの `assets` フォルダに格納し、アプリ起動時にパースしてリスト化します。
  - 特殊な難読化URLや動的解決が必要な場合は、バックグラウンド処理（Coroutines等）で抽出ロジック（または `youtubedl-android` のようなライブラリ）を使用してMediaItemに渡します。

### 1-3. 録画機能
- **Windows版**: `ffmpeg.exe -i [URL] -c copy` でストリームをそのままダンプ。
- **Android版**: AndroidネイティブでストリーミングTSを結合しながらダウンロードする場合、`Media3 DownloadManager`（HLSダウンロード機能）を使用するか、バックグラウンドワーカーでチャンクデータを内部ストレージ（メディアストア）に保存する実装が推奨されます。

## 2. アプリケーション構成

- **アーキテクチャ**: MVVM (Model-View-ViewModel) アーキテクチャ + Clean Architecture (任意)
- **UIフレームワーク**: Jetpack Compose を推奨。テレビアプリ(Android TV)対応も見据えてTV Composeの導入も視野に入れます。
- **通信レイヤー**: Retrofit + OkHttp (番組表や外部リストの取得用)
- **パーシステンス**: Room (視聴履歴やお気に入り機能の保存用)

## 3. 画面・UIの工夫ポイント

1. **シングル・ダブルタップ操作の統合**:
   Windows版で実装されていた「クリック操作による再生・フルスクリーン化・ミュート」の機能は、ExoPlayerの `PlayerView` 上でのジェスチャー操作（ダブルタップで10秒スキップ、ピンチズームなど）に置き換えます。
2. **映像・音声エフェクト (イコライザー等)**:
   ExoPlayerの `DefaultRenderersFactory` 経由で、AudioProcessorやVideoProcessorを挟むことで、Windows版で行なっていた「lavfi」のような各種エフェクト（ステレオ拡張、音量正規化等）を再現可能です。
3. **チャンネル切替ダイアログとテレビ欄**:
   再生画面にオーバーレイ表示されるBottom Sheet（またはSide Drawer）として、チャンネル一覧や「テレビ欄」画面を表示させ、シームレスなザッピング体験を提供します。
