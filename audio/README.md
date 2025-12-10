# 用户音频贡献指南

本分支仅用于存放用户贡献的音频资源（不包含应用代码）。

## 目录与命名
- 将文件放在 `audio/` 下的分类子目录（如 `rain/`、`nature/`、`urban/`）。
- 文件名使用英文小写与下划线，可附采样率或版本：`rain_loop_44k_v1.wav`。
- 建议单文件体积 < 20MB；优先提供可无缝循环的短片段。

## 许可与说明
- 仅接收可再分发/可商用的许可：MIT、CC0、Pixabay Content License 等。
- 请在对应目录放置简要说明（如 `README.md` 或 `info.txt`），至少包含：
  - 音频来源链接与作者
  - 许可类型
  - 时长、采样率
  - 是否无缝循环，必要时写明 loop 起止点

## Git LFS
- 仓库已为 `*.wav`、`*.mp3`、`*.ogg`、`*.flac` 配置 Git LFS；提交前确认 LFS 正常工作。
- 若本地未启用，运行一次 `git lfs install --local`，再提交文件。

## 提交流程
1) Fork 仓库并切到 `audio` 分支。  
2) 将音频与说明文件放入 `audio/` 目录。  
3) `git add audio/...`（连同说明与 `.gitattributes` 变更）。  
4) `git commit -m "Add rain loop audio (CC0)"`  
5) 发起针对 `audio` 分支的 Pull Request。

