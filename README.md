<div align="center">

# X Downloader

**Twitter/X Media Downloader for Android**

[![Build APK](https://github.com/Thewanwan/new-x-downloader/actions/workflows/build.yml/badge.svg)](https://github.com/Thewanwan/new-x-downloader/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)

[English](#english) | [中文](#中文) | [日本語](#日本語) | [한국어](#한국어)

</div>

---

## English

### Description

X Downloader is a powerful Android application for downloading media content (images, videos, GIFs) from Twitter/X. It supports multiple user accounts, incremental downloads, and keeps a local database to track download history.

### Features

- **Multi-User Support**: Add and manage multiple Twitter accounts
- **Incremental Download**: Only download content you haven't downloaded before
- **Full Re-download**: Option to re-download everything from scratch
- **Background Service**: Downloads continue even when the app is minimized
- **Download History**: SQLite database tracks all downloaded content
- **Progress Tracking**: Real-time download progress display

### Screenshots

| Home Screen | Settings | Download Progress |
|:-----------:|:--------:|:-----------------:|
| *User list with download buttons* | *Cookie configuration* | *Real-time progress* |

### Download

Download the latest APK from [GitHub Releases](https://github.com/Thewanwan/new-x-downloader/releases) or build it yourself.

### Getting Started

#### 1. Get Your Cookie

1. Open [x.com](https://x.com) in your browser and log in
2. Open Developer Tools (F12 or Ctrl+Shift+I)
3. Go to the **Network** tab
4. Refresh the page and click on any request
5. Find the `Cookie` field in the **Headers** section
6. Copy the value: `auth_token=xxx; ct0=xxx;`

#### 2. Configure the App

1. Open X Downloader
2. Tap the **Settings** icon (gear) in the top right
3. Paste your cookie in the **Cookie** field
4. Go back to the home screen

#### 3. Add Users and Download

1. Tap the **+** button to add a Twitter user
2. Enter the username (without @)
3. Tap **Download** on the user card
4. Choose download mode:
   - **Incremental**: Only new content
   - **Full**: Re-download everything
5. Wait for the download to complete

### Build from Source

```bash
# Clone the repository
git clone https://github.com/Thewanwan/new-x-downloader.git
cd new-x-downloader

# Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Requirements

- Android 8.0 (API level 26) or higher
- Internet connection
- Valid Twitter/X account cookie

### Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| Network | OkHttp |
| Async | Kotlin Coroutines |
| DI | Manual |
| Build | Gradle 8.11.1 |

### Project Structure

```
app/src/main/java/com/twitter/downloader/
├── data/
│   ├── local/          # Room database (users, downloads)
│   ├── remote/         # Twitter API client
│   └── repository/     # Data repositories
├── service/            # Background download service
├── ui/
│   ├── screens/        # App screens (Home, Settings)
│   ├── theme/          # Material 3 theme
│   └── components/     # Reusable UI components
└── util/               # Utility classes
```

### License

This project is for educational purposes only. Use responsibly and respect Twitter's Terms of Service.

---

## 中文

### 描述

X Downloader 是一款强大的 Android 应用，用于下载 Twitter/X 上的媒体内容（图片、视频、GIF）。支持多用户管理、增量下载，并维护本地数据库记录下载历史。

### 功能特性

- **多用户支持**：添加和管理多个推特账号
- **增量下载**：只下载之前未下载的内容
- **全部重新下载**：从头开始重新下载所有内容
- **后台服务**：应用最小化后下载继续进行
- **下载历史**：SQLite 数据库记录所有已下载内容
- **进度跟踪**：实时显示下载进度

### 截图

| 主页 | 设置 | 下载进度 |
|:----:|:----:|:--------:|
| *用户列表和下载按钮* | *Cookie 配置* | *实时进度* |

### 下载

从 [GitHub Releases](https://github.com/Thewanwan/new-x-downloader/releases) 下载最新 APK，或自行构建。

### 快速开始

#### 1. 获取 Cookie

1. 在浏览器中打开 [x.com](https://x.com) 并登录
2. 打开开发者工具（F12 或 Ctrl+Shift+I）
3. 切换到 **Network（网络）** 标签
4. 刷新页面，点击任意请求
5. 在 **Headers（请求头）** 中找到 `Cookie` 字段
6. 复制值：`auth_token=xxx; ct0=xxx;`

#### 2. 配置应用

1. 打开 X Downloader
2. 点击右上角的 **设置** 图标（齿轮）
3. 在 **Cookie** 字段粘贴你的 cookie
4. 返回主页

#### 3. 添加用户并下载

1. 点击 **+** 按钮添加推特用户
2. 输入用户名（不含 @）
3. 点击用户卡片上的 **下载**
4. 选择下载模式：
   - **仅下载未下载的**：只下载新内容
   - **全部重新下载**：重新下载所有内容
5. 等待下载完成

### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/Thewanwan/new-x-downloader.git
cd new-x-downloader

# 构建 debug APK
./gradlew assembleDebug

# APK 位置：
# app/build/outputs/apk/debug/app-debug.apk
```

### 系统要求

- Android 8.0（API 级别 26）或更高版本
- 网络连接
- 有效的 Twitter/X 账号 cookie

### 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 数据库 | Room (SQLite) |
| 网络 | OkHttp |
| 异步 | Kotlin 协程 |
| 构建 | Gradle 8.11.1 |

### 项目结构

```
app/src/main/java/com/twitter/downloader/
├── data/
│   ├── local/          # Room 数据库（用户、下载记录）
│   ├── remote/         # Twitter API 客户端
│   └── repository/     # 数据仓库
├── service/            # 后台下载服务
├── ui/
│   ├── screens/        # 应用页面（主页、设置）
│   ├── theme/          # Material 3 主题
│   └── components/     # 可复用 UI 组件
└── util/               # 工具类
```

### 许可证

本项目仅供学习交流使用。请合理使用，并遵守 Twitter 的服务条款。

---

## 日本語

### 説明

X Downloader は、Twitter/X のメディアコンテンツ（画像、動画、GIF）をダウンロードするための強力な Android アプリケーションです。複数ユーザーのサポート、インクリメンタルダウンロード、ダウンロード履歴のローカルデータベース管理に対応しています。

### 機能

- **マルチユーザーサポート**: 複数の Twitter アカウントの追加と管理
- **インクリメンタルダウンロード**: まだダウンロードしていないコンテンツのみダウンロード
- **フル再ダウンロード**: 最初からすべて再ダウンロードするオプション
- **バックグラウンドサービス**: アプリを最小化してもダウンロードが継続
- **ダウンロード履歴**: SQLite データベースですべてのダウンロードコンテンツを記録
- **進行状況追跡**: リアルタイムのダウンロード進行状況表示

### スクリーンショット

| ホーム画面 | 設定 | ダウンロード進行状況 |
|:----------:|:----:|:-------------------:|
| *ユーザーリストとダウンロードボタン* | *Cookie 設定* | *リアルタイム進行状況* |

### ダウンロード

[GitHub Releases](https://github.com/Thewanwan/new-x-downloader/releases) から最新 APK をダウンロードするか、自分でビルドしてください。

### はじめに

#### 1. Cookie を取得する

1. ブラウザで [x.com](https://x.com) を開き、ログイン
2. 開発者ツールを開く（F12 または Ctrl+Shift+I）
3. **Network** タブに切り替え
4. ページを更新し、任意のリクエストをクリック
5. **Headers** セクションで `Cookie` フィールドを見つける
6. 値をコピー：`auth_token=xxx; ct0=xxx;`

#### 2. アプリを設定する

1. X Downloader を開く
2. 右上の **設定** アイコン（歯車）をタップ
3. **Cookie** フィールドに cookie を貼り付け
4. ホーム画面に戻る

#### 3. ユーザーを追加してダウンロード

1. **+** ボタンをタップして Twitter ユーザーを追加
2. ユーザー名を入力（@ なし）
3. ユーザーカードの **ダウンロード** をタップ
4. ダウンロードモードを選択：
   - **インクリメンタル**: 新しいコンテンツのみ
   - **フル**: すべて再ダウンロード
5. ダウンロード完了を待つ

### ソースからビルド

```bash
# リポジトリをクローン
git clone https://github.com/Thewanwan/new-x-downloader.git
cd new-x-downloader

# デバッグ APK をビルド
./gradlew assembleDebug

# APK の場所：
# app/build/outputs/apk/debug/app-debug.apk
```

### システム要件

- Android 8.0（API レベル 26）以上
- インターネット接続
- 有効な Twitter/X アカウントの cookie

### 技術スタック

| コンポーネント | 技術 |
|--------------|------|
| 言語 | Kotlin |
| UI フレームワーク | Jetpack Compose + Material 3 |
| データベース | Room (SQLite) |
| ネットワーク | OkHttp |
| 非同期 | Kotlin コルーチン |
| ビルド | Gradle 8.11.1 |

### プロジェクト構造

```
app/src/main/java/com/twitter/downloader/
├── data/
│   ├── local/          # Room データベース（ユーザー、ダウンロード記録）
│   ├── remote/         # Twitter API クライアント
│   └── repository/     # データリポジトリ
├── service/            # バックグラウンドダウンロードサービス
├── ui/
│   ├── screens/        # アプリ画面（ホーム、設定）
│   ├── theme/          # Material 3 テーマ
│   └── components/     # 再利用可能な UI コンポーネント
└── util/               # ユーティリティクラス
```

### ライセンス

本プロジェクトは学習目的のみです。責任を持って使用し、Twitter の利用規約を尊重してください。

---

## 한국어

### 설명

X Downloader는 Twitter/X의 미디어 콘텐츠(이미지, 비디오, GIF)를 다운로드하기 위한 강력한 Android 애플리케이션입니다. 여러 사용자 계정 지원, 점진적 다운로드, 다운로드 기록을 추적하는 로컬 데이터베이스를 유지합니다.

### 기능

- **다중 사용자 지원**: 여러 Twitter 계정 추가 및 관리
- **점진적 다운로드**: 아직 다운로드하지 않은 콘텐츠만 다운로드
- **전체 재다운로드**: 처음부터 모든 것을 다시 다운로드하는 옵션
- **백그라운드 서비스**: 앱이 최소화되어도 다운로드가 계속됨
- **다운로드 기록**: SQLite 데이터베이스로 모든 다운로드 콘텐츠 기록
- **진행 상황 추적**: 실시간 다운로드 진행 상황 표시

### 스크린샷

| 홈 화면 | 설정 | 다운로드 진행 상황 |
|:--------:|:----:|:-----------------:|
| *사용자 목록과 다운로드 버튼* | *Cookie 설정* | *실시간 진행 상황* |

### 다운로드

[GitHub Releases](https://github.com/Thewanwan/new-x-downloader/releases)에서 최신 APK를 다운로드하거나 직접 빌드하세요.

### 시작하기

#### 1. Cookie 가져오기

1. 브라우저에서 [x.com](https://x.com)을 열고 로그인
2. 개발자 도구 열기 (F12 또는 Ctrl+Shift+I)
3. **Network** 탭으로 전환
4. 페이지를 새로고침하고 임의의 요청 클릭
5. **Headers** 섹션에서 `Cookie` 필드 찾기
6. 값 복사: `auth_token=xxx; ct0=xxx;`

#### 2. 앱 설정하기

1. X Downloader 열기
2. 오른쪽 상단의 **설정** 아이콘(톱니바퀴) 탭
3. **Cookie** 필드에 cookie 붙여넣기
4. 홈 화면으로 돌아가기

#### 3. 사용자 추가 및 다운로드

1. **+** 버튼을 탭하여 Twitter 사용자 추가
2. 사용자 이름 입력 (@ 제외)
3. 사용자 카드의 **다운로드** 탭
4. 다운로드 모드 선택:
   - **점진적**: 새 콘텐츠만
   - **전체**: 모든 것 재다운로드
5. 다운로드 완료 대기

### 소스에서 빌드

```bash
# 저장소 클론
git clone https://github.com/Thewanwan/new-x-downloader.git
cd new-x-downloader

# 디버그 APK 빌드
./gradlew assembleDebug

# APK 위치:
# app/build/outputs/apk/debug/app-debug.apk
```

### 시스템 요구사항

- Android 8.0(API 레벨 26) 이상
- 인터넷 연결
- 유효한 Twitter/X 계정 cookie

### 기술 스택

| 구성 요소 | 기술 |
|-----------|------|
| 언어 | Kotlin |
| UI 프레임워크 | Jetpack Compose + Material 3 |
| 데이터베이스 | Room (SQLite) |
| 네트워크 | OkHttp |
| 비동기 | Kotlin 코루틴 |
| 빌드 | Gradle 8.11.1 |

### 프로젝트 구조

```
app/src/main/java/com/twitter/downloader/
├── data/
│   ├── local/          # Room 데이터베이스 (사용자, 다운로드 기록)
│   ├── remote/         # Twitter API 클라이언트
│   └── repository/     # 데이터 리포지토리
├── service/            # 백그라운드 다운로드 서비스
├── ui/
│   ├── screens/        # 앱 화면 (홈, 설정)
│   ├── theme/          # Material 3 테마
│   └── components/     # 재사용 가능한 UI 컴포넌트
└── util/               # 유틸리티 클래스
```

### 라이선스

이 프로젝트는 교육 목적으로만 제공됩니다. 책임감을 가지고 사용하고 Twitter의 서비스 약관을 존중해 주세요.

---

<div align="center">

**Disclaimer**: This tool is for educational purposes only. Respect copyright and platform terms of service.

**免責事項**: このツールは教育目的のみです。著作権とプラットフォームの利用規約を尊重してください.

**면책 조항**: 이 도구는 교육 목적으로만 사용됩니다. 저작권과 플랫폼 이용 약관을 존중해 주세요.

**면책 조항**: 이 도구는 교육 목적으로만 사용됩니다. 저작권과 플랫폼 이용 약관을 존중해 주세요.

</div>
