# Git 提交规范工具

## 使用方法

### 方法1：使用交互式助手（推荐）

```bash
./.git-scripts/commit-helper.sh
```

按照提示选择类型、作用域、输入描述即可。

### 方法2：使用 git commit（会自动打开模板）

```bash
git add .
git commit
```

编辑器会自动显示提交模板，按照格式填写即可。

### 方法3：直接命令行

```bash
git commit -m "feat(audio): 添加雨声循环播放"
git commit -m "fix(ui): 修复深色模式按钮颜色"
git commit -m "docs: 更新README安装说明"
```

## 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat` - 新功能
- `fix` - Bug修复
- `docs` - 文档
- `style` - 代码格式
- `refactor` - 重构
- `perf` - 性能优化
- `test` - 测试
- `chore` - 构建/工具
- `ci` - CI配置
- `build` - 构建系统

### Scope 作用域（可选）

- `audio` - 音频管理
- `ui` - 界面
- `theme` - 主题
- `timer` - 倒计时
- `settings` - 设置
- `update` - 更新
- `cache` - 缓存

## 示例

```bash
# 新功能
feat(audio): 支持无缝循环播放
fix(timer): 修复后台计时器不工作的问题

# Bug修复  
fix: 修复崩溃问题
fix(ui): 修复深色模式下按钮颜色异常

# 文档
docs: 更新README安装步骤
docs: 添加API使用说明

# 重构
refactor(audio): 拆分AudioManager为多个模块
refactor: 优化代码结构

# 性能
perf(audio): 优化音频加载速度
perf: 减少内存占用
```

## 快捷别名（可选）

在 `~/.zshrc` 或 `~/.bashrc` 添加：

```bash
alias gcm='~/.git-scripts/commit-helper.sh'
```

然后就可以用 `gcm` 快速提交了。
