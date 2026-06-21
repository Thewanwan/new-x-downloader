# X Downloader

Twitter/X 媒体下载器 Android 应用。

## 功能

- 多用户下载
- 增量下载（仅下载未下载内容）
- 全部重新下载
- 后台下载服务
- 历史记录数据库

## 构建

```bash
# 使用 Android Studio 打开项目
# 或命令行构建
./gradlew assembleDebug
```

## 配置

1. 打开应用，进入设置页面
2. 填入 Cookie（从浏览器开发者工具获取）
3. 返回主页，点击 + 添加推特用户名
4. 选择下载方式，开始下载

## Cookie 获取方法

1. 登录 x.com
2. 打开开发者工具（F12）
3. 切换到 Network 标签
4. 刷新页面，找到任意请求
5. 在 Headers 中找到 Cookie 字段
6. 复制 `auth_token=xxx; ct0=xxx;` 部分

## 技术栈

- Kotlin
- Jetpack Compose (Material 3)
- Room Database
- OkHttp
- Coroutines
- DataStore
