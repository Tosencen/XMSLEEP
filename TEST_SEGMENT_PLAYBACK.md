# 分段播放功能测试指南

## 测试准备

### 1. 已完成的准备工作

✅ **音频片段已生成**
- 位置: `audio/rain/heavy_rain_segments/`
- 片段数量: 4 个
- 每个片段时长: 约 3.7 秒

✅ **资源文件已添加**
- `app/src/main/res/raw/heavy_rain_seg_0.ogg`
- `app/src/main/res/raw/heavy_rain_seg_1.ogg`
- `app/src/main/res/raw/heavy_rain_seg_2.ogg`
- `app/src/main/res/raw/heavy_rain_seg_3.ogg`

✅ **代码已集成**
- `AudioManager.initializeTestSegments()` 已在应用启动时调用
- 片段播放逻辑已实现

## 测试步骤

### 1. 重新构建项目

由于添加了新的资源文件，需要重新构建项目：

```bash
./gradlew clean assembleDebug
```

或者使用 Android Studio：
- Build → Clean Project
- Build → Rebuild Project

### 2. 安装并运行应用

```bash
./gradlew installDebug
```

### 3. 测试分段播放

1. 打开应用
2. 找到 "大雨" (Heavy Rain) 音频
3. 点击播放
4. **观察日志**：应该看到类似以下日志：
   ```
   AudioManager: HEAVY_RAIN 设置片段列表，共 4 个片段
   AudioManager: HEAVY_RAIN 使用片段播放，初始片段: heavy_rain_segment_0, heavy_rain_segment_1
   AudioManager: HEAVY_RAIN 队列下一个片段: heavy_rain_segment_2
   ```

### 4. 验证功能

**预期行为**：
- ✅ 播放时随机选择片段
- ✅ 每个片段播放完后自动切换到下一个片段
- ✅ 播放列表始终保持至少 2 个片段在队列中
- ✅ 播放流畅，无明显卡顿

**如何验证**：
- 观察播放是否流畅
- 检查 Logcat 中的日志，确认片段切换正常
- 播放一段时间，确认片段随机选择（不会总是按顺序播放）

## 查看日志

使用 adb 查看日志：

```bash
adb logcat | grep AudioManager
```

关键日志：
- `已初始化 heavy_rain 的片段播放（测试模式）`
- `HEAVY_RAIN 使用片段播放`
- `HEAVY_RAIN 队列下一个片段`

## 如果遇到问题

### 问题 1: 资源文件找不到

**症状**: 日志显示 "片段没有指定 localResourceId，跳过"

**解决**: 
1. 确认资源文件在 `app/src/main/res/raw/` 目录
2. 重新构建项目
3. 检查文件名是否符合 Android 资源命名规则（小写字母、数字、下划线）

### 问题 2: 播放失败

**症状**: 音频无法播放

**解决**:
1. 检查 Logcat 中的错误信息
2. 确认片段文件格式正确（OGG Vorbis）
3. 尝试播放原始 `heavy_rain.ogg` 文件，确认不是资源文件本身的问题

### 问题 3: 片段不切换

**症状**: 只播放一个片段，不切换到下一个

**解决**:
1. 检查 `onMediaItemTransition` 是否被调用
2. 确认播放列表管理逻辑正常
3. 查看日志确认 `queueNextLocalSegment` 是否被调用

## 测试结果记录

测试日期: ___________

测试结果:
- [ ] 播放正常
- [ ] 片段随机选择正常
- [ ] 片段切换流畅
- [ ] 无明显卡顿或延迟

备注:
_________________________________

## 下一步

如果测试成功，可以考虑：
1. 为更多音频添加片段（使用相同的脚本）
2. 优化片段选择算法
3. 添加桥接片段支持（高级功能）

如果测试失败，请记录错误信息并检查上述问题排查步骤。

