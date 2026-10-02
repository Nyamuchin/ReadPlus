# ReadPlus

一款轻量的本地漫画 / 视频阅读器，专注于**本地媒体管理**和**沉浸式阅读/播放体验**。

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12.01-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![minSdk](https://img.shields.io/badge/minSdk-26-orange.svg)](https://developer.android.com/about/versions/oreo)

---

## 📖 项目简介

ReadPlus 是一款完全本地化的漫画和视频管理工具。它不做任何在线内容，只专注于**读好你自己的漫画**和**看好你自己的视频**。

- 漫画部分支持 ZIP 打包导入，提供三种阅读模式（含日漫从右到左），双指缩放 + 缩略图快速跳页
- 视频部分支持竖屏瀑布流播放，滑动切换、毫秒级进度条、单击暂停
- 漫画和视频各自维护独立的分类体系
- 支持**完整数据备份**（含媒体文件），一键迁移到新设备

---

## ✨ 功能特性

### 📚 漫画

| 功能 | 说明 |
|------|------|
| **ZIP 导入** | 支持 `.zip` 打包的图片集，图片格式支持 `jpg` / `jpeg` / `png` / `webp` / `gif` |
| **自然排序** | `page_2.jpg` < `page_10.jpg`，自动识别数字序号 |
| **嵌套平铺** | ZIP 内的子文件夹自动平铺，按文件名排序 |
| **三种阅读模式** | 从左到右 / 从右到左（日漫）/ 纵向滚动 |
| **双指缩放** | 支持 1x ~ 5x 自由缩放，双击放大到 2.5x |
| **缩略图跳页** | 详情页 4 列缩略图，点击任意页直接跳转 |
| **阅读进度** | 自动保存，下次打开从上次位置继续 |
| **沉浸式全屏** | 阅读时隐藏状态栏和导航栏，单击切换工具栏 |
| **封面提取** | 自动从 ZIP 第一页提取封面 |

### 🎬 视频

| 功能 | 说明 |
|------|------|
| **多格式支持** | `mp4` / `mkv` / `webm` / `avi` |
| **两种导入方式** | SAF 文件夹授权自动扫描 / 手动多选文件 |
| **竖屏瀑布流** | 上滑切换下一个，全屏沉浸式播放 |
| **单视频循环** | 每个视频播完自动从头开始 |
| **毫秒级进度条** | 暂停时展开，播放时贴底细线，支持拖动预览 |
| **单击暂停/继续** | 暂停时中央显示播放按钮（抖音风格） |
| **顶部信息** | 暂停时显示视频标题和所属分类 |
| **封面缓存** | 提取视频第一帧作为封面，避免切换时黑屏 |

### 🗂 分类管理

- 漫画和视频各自维护**独立的分类体系**
- 支持新建 / 删除分类
- 长按内容卡片可"添加到分类"
- 按分类筛选查看

### 💾 数据备份

- **完整导出**：将漫画 ZIP、视频文件、封面、分类、阅读进度打包成一个 ZIP
- **完整导入**：一键恢复到新设备
- **进度反馈**：导出/导入过程显示实时进度

### 🎨 主题

- **Material 3**：Tonal Spot 调色板 + 2025 Expressive 色彩规范，Android 12+ 支持动态取色
- **Miuix**：HyperOS / MIUI 风格，大圆角扁平卡片
- 支持浅色 / 深色模式跟随系统

---

## 🛠 技术栈

| 分类 | 技术 |
|------|------|
| **语言** | Kotlin 2.0.21 |
| **UI** | Jetpack Compose + Material 3 |
| **架构** | MVVM + Clean Architecture（data / domain / ui 三层） |
| **依赖注入** | Hilt 2.53 |
| **数据库** | Room 2.6.1 |
| **视频播放** | AndroidX Media3 ExoPlayer 1.5.1 |
| **图片加载** | Coil 2.7.0（支持 gif / webp） |
| **偏好存储** | DataStore Preferences 1.1.1 |
| **导航** | Navigation Compose 2.8.5 |
| **序列化** | Gson 2.11.0（备份数据） |
| **构建** | AGP 8.7.3 + Gradle 8.9 |

---

## 📦 环境要求

| 组件 | 版本 |
|------|------|
| Android Studio | Ladybug (2024.3.1) 或更新 |
| JDK | 17 |
| Kotlin | 2.0.21 |
| AGP | 8.7.3 |
| Gradle | 8.9 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 (Android 15) |

---

## 🚀 构建与运行

### 1. 克隆仓库

```bash
git clone https://github.com/Nyamuchin/ReadPlus.git
cd ReadPlus