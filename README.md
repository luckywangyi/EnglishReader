# English Reader - 英文阅读学习应用

一款专为英语学习者设计的 Android 沉浸式阅读应用，帮助你通过阅读高质量英文内容提升英语能力。

## 核心特性

### 📚 多源英文内容
- 自动拉取 BBC、Guardian、NPR、Wired 等优质英文媒体内容
- 支持新闻、科技、演讲、科学等多个分类
- RSS 订阅自动更新，内容持续丰富

### 📖 沉浸式阅读体验
- 清爽无干扰的阅读界面
- 阅读进度自动保存
- 可调节字体大小
- 支持夜间模式

### 🔤 圈词翻译（重点功能）
- 长按或选择文本即时翻译
- 本地词典优先，秒速响应
- AI 翻译补充短语和句子
- 一键保存到词汇本

### 🤖 AI 智能辅助
- 文章难度自动评估（初级/中级/中高级/高级）
- AI 生成中英文摘要
- 关键词汇自动提取
- 阅读理解测试题目

### 📊 学习统计
- 已读文章统计
- 阅读单词量统计
- 连续阅读天数
- 词汇掌握进度

## 技术栈

- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **本地存储**: Room
- **网络**: Retrofit + OkHttp
- **RSS 解析**: RssParser
- **HTML 解析**: Jsoup
- **AI**: Google Gemini API（免费额度）

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Kotlin 1.9.22

### 构建步骤

1. 克隆项目
```bash
git clone <repository-url>
cd EnglishReader
```

2. 用 Android Studio 打开项目

3. 等待 Gradle 同步完成

4. 运行到设备或模拟器

### 配置 Gemini API

1. 访问 [Google AI Studio](https://aistudio.google.com)
2. 使用 Google 账号登录
3. 点击 "Get API Key" 获取免费 API Key
4. 在应用设置中填入 API Key

**免费额度：**
- 每分钟 15 次请求
- 每天 1500 次请求
- 个人学习完全够用

## 项目结构

```
app/src/main/java/com/englishreader/
├── di/                     # Hilt 依赖注入
├── data/
│   ├── local/
│   │   ├── dao/            # Room DAO
│   │   ├── entity/         # 数据库实体
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── rss/            # RSS 解析
│   │   └── gemini/         # Gemini API
│   └── repository/         # 数据仓库
├── domain/
│   ├── model/              # 领域模型
│   └── usecase/            # 用例
├── ui/
│   ├── theme/              # Material 3 主题
│   ├── components/         # 通用组件
│   ├── screens/
│   │   ├── home/           # 首页
│   │   ├── reader/         # 阅读页
│   │   ├── vocabulary/     # 词汇本
│   │   ├── stats/          # 统计
│   │   └── settings/       # 设置
│   └── navigation/
└── EnglishReaderApp.kt
```

## 内容来源

应用支持以下 RSS 订阅源：

**新闻类**
- BBC News
- The Guardian
- NPR News
- Reuters

**科技类**
- Wired
- Ars Technica
- The Verge
- TechCrunch

**科学类**
- Science Daily
- Phys.org
- National Geographic

**演讲类**
- TED Talks

**文化类**
- Aeon Essays
- Medium

## 版权说明

- 所有内容仅用于个人学习阅读
- 内容版权归原作者和媒体所有
- 不进行任何商业用途或二次分发
- 建议点击原文链接支持内容创作者

## 许可证

本项目采用 MIT 许可证。
