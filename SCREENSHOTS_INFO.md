# 应用截图说明

## 📸 截图位置

应用截图已经按照 F-Droid 和 IzzyOnDroid 的要求放置在正确的位置：

### Fastlane 目录结构

```
fastlane/metadata/android/
├── en-US/
│   └── images/
│       └── phoneScreenshots/
│           ├── 1.png
│           ├── 2.png
│           ├── 3.png
│           ├── 4.png
│           ├── 5.png
│           └── 6.png
├── zh-CN/
│   └── images/
│       └── phoneScreenshots/
│           └── (同样的截图文件)
└── zh-TW/
    └── images/
        └── phoneScreenshots/
            └── (同样的截图文件)
```

### 原始截图位置

原始截图保存在项目根目录的 `screenshots/` 目录下：
- `screenshots/1.jpg` 到 `screenshots/6.jpg`

## 📋 截图要求

### F-Droid 要求

1. **格式**：推荐 PNG 格式，JPG 也可以接受
2. **数量**：最多 6 张截图
3. **命名**：使用数字命名，如 `1.png`, `2.png`, `3.png` 等
4. **位置**：`fastlane/metadata/android/<locale>/images/phoneScreenshots/`
5. **语言**：可以为不同语言提供不同的截图（可选）

### 当前截图

- **数量**：6 张截图
- **格式**：PNG（从 JPG 复制并重命名）
- **语言**：已为 en-US、zh-CN、zh-TW 三个语言版本提供截图

## ✅ 截图状态

- [x] 截图已放置在正确的位置
- [x] 已为所有支持的语言提供截图
- [x] 截图命名符合要求（1.png 到 6.png）

## 📝 注意事项

1. **格式转换**：当前截图文件扩展名为 `.png`，但实际内容可能还是 JPG 格式。如果 F-Droid 审核时要求真正的 PNG 格式，可以使用图像编辑工具转换。

2. **截图更新**：如果需要更新截图：
   - 更新 `screenshots/` 目录下的原始文件
   - 然后复制到各个语言的 `phoneScreenshots/` 目录

3. **截图内容**：确保截图展示应用的主要功能和界面，帮助用户了解应用。

## 🔗 相关文档

- [F-Droid 截图文档](https://f-droid.org/zh_Hans/docs/All_About_Descriptions_Graphics_and_Screenshots/)
- [IzzyOnDroid Fastlane 文档](https://gitlab.com/IzzyOnDroid/repo/-/wikis/Docs/fastlane)

