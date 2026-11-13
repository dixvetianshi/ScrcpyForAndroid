<!-- markdownlint-disable MD033 -->

# Scrcpy for Android

<a name="tablet-main-display"></a>

## 本分支：手机主屏适配平板

`codex/temporary-display-size` 基于 [Miuzarte/ScrcpyForAndroid](https://github.com/Miuzarte/ScrcpyForAndroid)，新增可选的临时主屏分辨率调整，适合“手机运行游戏，平板显示和操作”。功能默认关闭，启用后在投屏前保存手机原来的分辨率设置，结束会话后恢复；不修改 DPI。

### 开启方式

1. 在平板安装本分支构建的 **Scrcpy 主屏适配**，通过 USB 或无线 ADB 连接手机。
2. 打开 **全部选项 → 临时调整手机主屏分辨率**。视频源需为屏幕、显示器选择默认主屏，并保持 `new-display` 为空。
3. 宽高留空时，每次开始投屏自动使用平板尺寸；也可填写自定义像素宽高，或点击“使用本机尺寸”。

尺寸会按手机的自然横竖方向映射。例如横屏平板的 1920×1200，在常见竖屏手机上对应 1200×1920。设置在下一次开始投屏时生效。

这里的“自动”按钮会清空自定义宽高，选择开始投屏时自动匹配平板。原有的“自动调整尺寸以匹配界面”（`--flex-display`）配合 `new-display` 调整独立虚拟屏，跟随窗口尺寸变化。使用本分支的主屏模式时，关闭 `--flex-display`，保持 `new-display` 为空。

### 恢复与验证范围

- 原来有 `wm size` 覆盖值时恢复该值；没有覆盖值时恢复系统默认。原值先持久保存，再修改手机。
- 启动失败、取消和正常结束会话都会尝试恢复。拔线或网络中断使命令无法送达时，保留待恢复记录，重新连接同一手机后先重试。
- 通过手机串号识别恢复目标；无法确认身份时不修改分辨率。其他操作已改变手机尺寸时保留原值和当前设置，避免覆盖后续更改。
- 已通过完整单元测试套件的 **64 项测试**，以及主应用 Kotlin/Java、ARM64/ARM32 JNI 和 Debug APK 构建；APK v2 签名验证通过。
- 尚未做真机安装或运行验证，具体 ROM、游戏、手柄及音频延迟仍需实测。其他投屏和输入功能沿用上游实现。

恢复逻辑见 [TemporaryDisplayResize.kt](app/src/main/java/io/github/miuzarte/scrcpyforandroid/scrcpy/display/TemporaryDisplayResize.kt)，相关回归见 [分辨率恢复测试](app/src/test/java/io/github/miuzarte/scrcpyforandroid/scrcpy/display/TemporaryDisplayResizeTest.kt)。

本分支的 Debug 包名是 `io.github.miuzarte.scrcpyforandroid.displayresize`，版本为 `0.7.3_pre1-displayresize`，最低 Android 8，ARM 通用构建包含 32/64 位库，可与原版并存。APK 是构建输出，不提交到 Git；构建方法见[构建本分支](#构建本分支)。

<a href="https://github.com/Genymobile/scrcpy/blob/master/app/data/icon.svg" title="Modified from the original version">
  <img src="app/src/main/assets/icon/icon.svg" width="128" height="128" alt="scrcpy" align="right" />
</a>

[scrcpy](https://github.com/Genymobile/scrcpy) android client

从通过
[ADB Wireless](https://developer.android.com/tools/adb?hl=zh-cn#connect-to-a-device-over-wi-fi)
连接的 Android 设备镜像视频与音频，并允许使用触摸屏与键盘鼠标进行控制

不需要 root 权限，也无需在设备上安装应用程序

> [!NOTE]
> 本项目基于 scrcpy，但并非其官方版本，与原作者及维护团队不存在任何隶属或合作关系

## 下载

**本分支 APK**：[预览版发布页](https://github.com/dixvetianshi/ScrcpyForAndroid/releases/tag/v0.7.3_pre1-displayresize) · [直接下载 ARM 通用 APK](https://github.com/dixvetianshi/ScrcpyForAndroid/releases/download/v0.7.3_pre1-displayresize/Scrcpy-Display-Resize.apk)

应用名称为“Scrcpy 主屏适配”，支持 Android 8 及以上、ARM32/ARM64，可与原版并存。发布说明包含文件校验值和验证范围，尚未进行真机运行验证。

\> [上游 releases](https://github.com/Miuzarte/ScrcpyForAndroid/releases)

此链接提供上游发布版。要使用本分支新增的“临时调整手机主屏分辨率”开关，请使用本分支构建的 APK。

## 截图

<p align="center">
  <img src="https://github.com/user-attachments/assets/00bf5e7c-2e37-4e99-976e-06a56232a628" height="300" alt="Devices" />
  <img src="https://github.com/user-attachments/assets/a423cf0e-5514-423e-8daf-818add2b5558" height="300" alt="Recent Tasks" />
  <img src="https://github.com/user-attachments/assets/f1271cc7-ce45-46d2-8a01-0f4a367c0e4a" height="300" alt="Streaming" />
  <img src="https://github.com/user-attachments/assets/220fc973-f3b7-4bec-8733-95bbddf95a7a" height="300" alt="Scrcpy All Options" />
  <img src="https://github.com/user-attachments/assets/6d4cff1b-1277-44e4-bcfa-18201738a703" height="300" alt="Terminal" />
  <img src="https://github.com/user-attachments/assets/3e4edc6c-ddfc-478b-aa7d-aa797a9f0e74" height="300" alt="Files" />
  <img src="https://github.com/user-attachments/assets/d3b29861-6e86-4301-9b1d-1f7836ad9b7e" height="300" alt="Settings" />
  <img src="https://github.com/user-attachments/assets/f513b7ba-0389-4176-8382-c1a08c4eba99" height="300" alt="Multi Touch Test" />
  <img src="https://github.com/user-attachments/assets/7a50bd1f-8095-4269-8e58-88316d86e3d8" height="300" alt="Virtual Buttons Reorder" />
  <img src="https://github.com/user-attachments/assets/448481df-c15b-4580-af96-141e9b56c41f" height="300" alt="About" />
</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/558bd1b1-15d2-47f8-bdc5-aac9cca689f5" height="180" alt="PiP" />
  <img src="https://github.com/user-attachments/assets/1b52a662-108d-49cb-a86e-eccc8ac12d64" height="180" alt="Gaming" />
</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/59116f01-8163-417b-8f2b-45c5c624a37a" height="180" alt="Gamepad" />
</p>

## Features

- 控制时可拉起本机输入法，且支持输入中文
- 剪贴板同步
- 低延迟音频链路 (默认未启用)
  - 受控设备播放 `USAGE_MEDIA` 流时 ([namidaco/namida](https://github.com/namidaco/namida)) ，两设备的音频延迟只差半拍 (没有具体测量能力)
  - 受控设备播放 `USAGE_GAME` 流时 (明日方舟 Bilibili 服) ，仍存在 100~200ms 的有感延迟
- 带生物认证的锁屏密码自动填充 (入口位于虚拟按钮中)
- 多配置切换，设备绑定配置，连接后直接进入全屏
- 临时调整手机主屏分辨率（默认关闭）
  - 用法和恢复范围见[本分支说明](#tablet-main-display)
- 可替换 scrcpy-server
- 利用 mDNS 服务实现自动连接启用无线调试的设备、自动发现等待配对设备的IP与端口
- 二维码配对
- 自动横竖屏切换
- 横屏布局
  - 仅屏幕比例小于 16:9 的设备
- 全屏下映射返回键到远程
- 画中画
- 双向文件传输
- 流式 adb 终端
- 内置录制

## 已知问题

- 因缺乏精力维护，AI 参与维护的占比有极大提升
- 刚开始串流时有概率丢失关键帧导致花屏，等待一段时间后重新接收关键帧即可
- 因为没有设备用于 (也懒得) 测试，应用可能无法正常运行在安卓版本较低的设备上，特别是画中画功能，非常取决于国产 ROM 的实现
- 关闭画中画后不会停止 scrcpy 串流，仍然需要回到应用中点击停止
- 跨设备输入中文
  - 实现方式为利用剪贴板同步，会导致受控机剪贴板历史被填充输入历史
  - 不知道为什么有时候会上屏失败
- 虚拟按键的截图实现方式为发送
`keycode 120`，安卓官方([keycodes.h#349](https://android.googlesource.com/platform/frameworks/native/+/master/include/android/keycodes.h#349))的定义为
`System Request / Print Screen key.`，不同的厂商有不同的实现，在某些类原生(`AxionOS`) 上的行为是软重启
- 切换语言后需要重启应用才能进入全屏

## TODO

\> [TODO.md](TODO.md)

## NOT-TODO

- 低版本安卓适配
  - 项目的 `minSdk` 为 26 / Android 8，98.4% 的设备都能成功安装上，~~只是出问题不管~~
  出问题了 (特别是崩溃闪退) 带 logcat 来尽量修
- 更多的文件操作，包括不限于 `删除`, `重命名` 等
  - 可以长按复制路径去终端用 `rm`, `mv`
- 录制的进一步优化
  - `MediaCodec` 限制太大，再继续做要引入 `ffmpeg`，ofc no way
- ADB 安装应用 / adb install
  - 需要大改 JNI 的实现因此不做
  - 可以推送文件之后使用终端安装或手动控制安装

## Change Log

\> [CHANGELOG.md](CHANGELOG.md)

## 建议搭配模块

- 密码锁屏无法捕获: [LSPosed/DisableFlagSecure](https://github.com/LSPosed/DisableFlagSecure)
- 开机自动启用 adb: [gist/906291](https://gist.github.com/Miuzarte/9062915f1615d5eebd363c759fda496c)

## FAQ

0. 控制不了
   - [Genymobile/scrcpy/FAQ.md#control-issues](https://github.com/Genymobile/scrcpy/blob/master/FAQ.md#control-issues)

1. 切到后台后 ADB 断连
   - 将国产 ROM 中的 `省电策略` 调整至 `无限制`
   - 将安卓原生设置中的 `允许后台使用` 启用并设置为 `无限制` (应用设置页中有入口)

2. 虚拟屏不显示输入法 / 输入法显示在主屏幕
   - 将 `--display-ime-policy` 设置为 `local`
   - 自行在悬浮球中拉起本机输入法

3. 码率只能拉到 40Mbps 嫌低
   - 每个 Slider 选项的标题都可以点开自己输入值

4. 录制/下载的文件在哪
   - 视频录制: /sdcard/Movies/Scrcpy/
   - 音频录制: /sdcard/Music/Scrcpy/
   - 下载: /sdcard/Download/Scrcpy/

5. 横屏模式对左撇子不太友好
   - 右上角有按钮可以对调方向

6. MIUI 虚拟屏没有桌面 / 虚拟屏白屏
   - 装个第三方桌面

## 构建

- JDK 21
- Android SDK (`compileSdk 37` / `buildTools 37.0.0`)
- Android NDK `30.0.16248370` (需要与 `libcxx` 一致)

### 构建本分支

先克隆本分支及其子模块，再构建 ARM 通用包：

```bash
git clone --branch codex/temporary-display-size --recurse-submodules https://github.com/dixvetianshi/ScrcpyForAndroid.git
cd ScrcpyForAndroid
git submodule update --init --recursive
./gradlew :app:testDebugUnitTest :app:assembleDebug "-PabiList=arm64-v8a,armeabi-v7a" --max-workers=2
```

Windows PowerShell 使用：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug "-PabiList=arm64-v8a,armeabi-v7a" --max-workers=2
```

ARM 通用 APK 输出到 `app/build/outputs/apk/debug/app-universal-debug.apk`。设置 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 指向已安装的 SDK，确保 miuix 的 included build 也能找到 SDK。Windows 构建使用无中文路径的检出目录。

### 上游构建示例

```bash
git clone --recursive https://github.com/Miuzarte/ScrcpyForAndroid.git
cd ScrcpyForAndroid
./gradlew assembleDebug
```

已克隆但没有拉取子模块：

```bash
git submodule update --init --recursive
```

specific abi:

```bash
./gradlew assembleRelease -PabiList=arm64-v8a
```

## Credits

- [Genymobile/scrcpy](https://github.com/Genymobile/scrcpy) (包括图标)
- JNI ADB 实现: [rikkaapps/shizuku](https://github.com/rikkaapps/shizuku), [vvb2060/ndk.boringssl](https://github.com/vvb2060), [lsposed/libcxx](https://github.com/lsposed/libcxx)
- 界面组件: [YuKongA/miuix](https://github.com/compose-miuix-ui/miuix)
- 界面设计参考: [tiann/KernelSU/manager](https://github.com/tiann/KernelSU/tree/main/manager), [miuix/example](https://github.com/compose-miuix-ui/miuix/tree/main/example)
- 画中画实现参考: [ClassicOldSong/moonlight-android](https://github.com/ClassicOldSong/moonlight-android)
- 原生应用设置页跳转: [YifePlayte/WOMMO](https://github.com/YifePlayte/WOMMO)
- 终端实现: [reapercanuk39/termux-kotlin-app](https://github.com/reapercanuk39/termux-kotlin-app) (仅 Apache 2.0 部分)
- 二维码生成: [nayuki/QR-Code-generator](https://github.com/nayuki/QR-Code-generator/tree/master/java)

## License

[Apache License 2.0](LICENSE)

## Star History

<a href="https://www.star-history.com/?repos=Miuzarte%2FScrcpyForAndroid&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=Miuzarte/ScrcpyForAndroid&type=date&theme=dark&legend=top-left&sealed_token=ZAxkizLKrqW0OrnbwXmuzTskU0mzMsjF--hGG8WW4F38bJGglf17mqXYZ6aQvePlP7ocCCS39PHNQYgjyLIEGcbU_8qQYXZ-YPs5N8slD0MphyJmujabc0AUKWMIpdq6iqSGifrLx-rQGBd26YTwEPikYV6SKjGVAxPhoMmMgzyJ13RtkP3rSm4-E2sN" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=Miuzarte/ScrcpyForAndroid&type=date&legend=top-left&sealed_token=ZAxkizLKrqW0OrnbwXmuzTskU0mzMsjF--hGG8WW4F38bJGglf17mqXYZ6aQvePlP7ocCCS39PHNQYgjyLIEGcbU_8qQYXZ-YPs5N8slD0MphyJmujabc0AUKWMIpdq6iqSGifrLx-rQGBd26YTwEPikYV6SKjGVAxPhoMmMgzyJ13RtkP3rSm4-E2sN" />
   <img alt="Star History Chart" src="https://api.star-history.com/chart?repos=Miuzarte/ScrcpyForAndroid&type=date&legend=top-left&sealed_token=ZAxkizLKrqW0OrnbwXmuzTskU0mzMsjF--hGG8WW4F38bJGglf17mqXYZ6aQvePlP7ocCCS39PHNQYgjyLIEGcbU_8qQYXZ-YPs5N8slD0MphyJmujabc0AUKWMIpdq6iqSGifrLx-rQGBd26YTwEPikYV6SKjGVAxPhoMmMgzyJ13RtkP3rSm4-E2sN" />
 </picture>
</a>
