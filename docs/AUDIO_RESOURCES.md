# 音频资源管理说明

## 概述

本项目参考了 [Moodist](https://github.com/remvze/moodist) 项目的音频资源管理方式，使用两种音频资源来源：

- **首页（白噪音页面）**：使用内置音频资源（打包在APK中）
- **星空页面**：使用GitHub上的网络音频资源（动态下载）

## 音频资源许可

本项目使用的音频资源来自第三方提供商，受不同的许可约束：

- **Pixabay Content License**：允许免费使用，包括研究用途和开源项目
- **CC0 (Creative Commons Zero)**：完全公共领域，可自由使用

详细许可信息请参考：
- [Pixabay Content License](https://pixabay.com/service/license-summary/)
- [Creative Commons Zero License](https://creativecommons.org/publicdomain/zero/1.0/)

## 音频资源结构

### GitHub 仓库结构

```
XMSLEEP/
├── audio/                    # 音频资源目录
│   ├── nature/              # 自然音
│   │   ├── river.ogg
│   │   ├── waves.ogg
│   │   └── ...
│   ├── rain/                # 雨声
│   │   ├── light-rain.ogg
│   │   ├── heavy-rain.ogg
│   │   └── ...
│   ├── animals/              # 动物音
│   ├── urban/                # 城市音
│   ├── places/               # 场所音
│   ├── transport/            # 交通音
│   ├── things/               # 物品音
│   └── noise/                # 噪音
│       ├── white-noise.ogg
│       ├── pink-noise.ogg
│       └── brown-noise.ogg
├── sounds_remote.json        # 网络音频清单文件
└── README.md                 # 项目说明（包含许可信息）
```

### 音频清单文件

`sounds_remote.json` 文件包含所有网络音频资源的元数据：

```json
{
  "version": "1.0.0",
  "categories": [
    {
      "id": "nature",
      "name": "自然",
      "nameEn": "Nature",
      "icon": "🌲",
      "order": 1
    }
  ],
  "sounds": [
    {
      "id": "river",
      "name": "河流",
      "nameEn": "River",
      "category": "nature",
      "icon": "🌊",
      "source": "REMOTE",
      "remoteUrl": "https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/audio/nature/river.ogg",
      "loopStart": 500,
      "loopEnd": 60000,
      "isSeamless": true,
      "format": "ogg",
      "order": 1
    }
  ]
}
```

## 音频文件要求

### 格式要求

- **推荐格式**：OGG Vorbis
- **采样率**：44.1 kHz
- **位深度**：16 bit
- **声道**：单声道（Mono）或立体声（Stereo）
- **比特率**：96-128 kbps（单声道），128-192 kbps（立体声）

### 循环要求

- **无缝循环**：音频文件的首尾必须能够无缝衔接
- **循环段长度**：建议 1-5 分钟
- **循环点**：在音频清单中指定 `loopStart` 和 `loopEnd`（毫秒）

### 文件命名

- 使用小写字母和连字符（kebab-case）
- 例如：`light-rain.ogg`、`white-noise.ogg`

## 添加新音频

### 步骤 1：准备音频文件

1. 确保音频文件已预处理为无缝循环
2. 转换为 OGG Vorbis 格式
3. 优化文件大小（单声道，96-128 kbps）

### 步骤 2：上传到 GitHub

1. 在 GitHub 仓库中创建对应的分类目录（如果不存在）
2. 上传音频文件到对应目录
3. 确保文件路径与清单中的 `remoteUrl` 一致

### 步骤 3：更新音频清单

1. 编辑 `sounds_remote.json` 文件
2. 添加新音频的元数据
3. 确保 `remoteUrl` 指向正确的 GitHub raw URL

### 步骤 4：测试

1. 在应用中测试音频下载和播放
2. 验证循环播放是否无缝
3. 检查缓存是否正常工作

## GitHub Raw URL 格式

音频文件的 GitHub Raw URL 格式：

```
https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/audio/[分类]/[文件名]
```

例如：
```
https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/audio/nature/river.ogg
```

## 参考项目

本项目参考了以下项目的音频资源管理方式：

- [Moodist](https://github.com/remvze/moodist) - 环境音效平台
  - 使用 MIT License（代码部分）
  - 音频资源使用 Pixabay Content License 和 CC0

## 注意事项

1. **许可合规**：确保所有音频资源都有明确的许可信息
2. **文件大小**：优化音频文件大小，减少下载时间
3. **循环质量**：确保音频文件首尾无缝衔接
4. **缓存管理**：合理设置缓存大小和策略
5. **错误处理**：处理网络错误和下载失败的情况

## 贡献

欢迎提交新的音频资源！请确保：

1. 音频资源有明确的许可信息（CC0 或 Pixabay Content License）
2. 音频文件已预处理为无缝循环
3. 文件格式符合要求（OGG Vorbis）
4. 更新 `sounds_remote.json` 文件

