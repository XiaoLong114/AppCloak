# 应用隐身衣（AppCloak）

一款面向 **已 root 安卓设备** 的应用隐藏工具。基于 Kotlin + Jetpack Compose，通过 `su` 执行系统命令，让你可以把指定的应用从桌面上藏起来，并提供系统生物识别作为管理入口的门禁。

> 本项目是 root 工具，不面向应用商店分发。请自行评估风险。

---

## 两种隐藏模式

| 模式 | 原理 | 应用还能运行吗 | 适用场景 |
| --- | --- | --- | --- |
| **禁用应用**（默认） | `pm disable-user --user 0 <包名>` | 否，完全无法启动、收不到通知，从所有桌面消失 | 最通用可靠，类似「冰箱」类工具 |
| **桌面数据库** | root 下用 `sqlite3` 修改桌面 App 自己的数据库 | 是，只是不在抽屉里显示 | 想保留应用运行（如收消息）但不想被看到 |

在「设置」页可随时切换模式。每条隐藏记录会记住自己使用的模式，恢复时按原模式执行，因此切换模式不会导致已有隐藏项丢失。

### 桌面数据库模式的注意事项

不同桌面（Launcher3 / MIUI / EMUI / ColorOS 等）的数据库结构和字段名差异很大，**没有万能 SQL**。本项目提供可编辑的 SQL 模板：

- `查询已隐藏`：执行后每行输出一个包名
- `隐藏 SQL` / `恢复 SQL`：用 `{pkg}` 作为包名占位符

配置步骤大致是：

1. 用 root 文件管理器找到桌面 App 的 `databases/` 目录，确认数据库文件名
2. 用 sqlite3 打开数据库，观察表和字段（`.tables`、`.schema`）
3. 在 App 的设置 → 桌面数据库配置中新建配置，填入包名、数据库相对路径和三条 SQL
4. 先用「测试查询」验证，再切到「桌面数据库」模式使用

设备上需要能执行 `sqlite3`。很多 ROM 不带它，可通过 Magisk 模块安装。该模式会在执行后 `am force-stop` 桌面进程以刷新界面；部分桌面可能需要手动重启或重新进入桌面才生效。

---

## 编译

当前环境没有 Java / Gradle / Android SDK，本项目只提供源码，需要在本地编译：

1. 安装 [Android Studio](https://developer.android.com/studio)（Ladybug 或更新版本），安装 Android SDK 34 与 JDK 17
2. 用 Android Studio 打开 `AppCloak` 目录（根目录的 `settings.gradle.kts` 所在目录）
3. 如提示缺少 `local.properties`，可复制 `local.properties.example` 并改成自己的 SDK 路径（Android Studio 通常会自动生成）
4. 等待 Gradle 同步完成，菜单 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. 产物在 `app/build/outputs/apk/debug/app-debug.apk`

命令行方式（需本机已配置 Android SDK 和 JDK 17）：

```bash
./gradlew assembleDebug
```

版本组合：AGP 8.5.2 / Kotlin 1.9.24 / Compose Compiler 1.5.14 / Gradle 8.7 / compileSdk 34 / minSdk 23。

---

## 使用

1. 安装后在 root 设备上打开，首次执行隐藏操作时 Magisk / KernelSU 会弹出 su 授权请求，请允许
2. 首页「应用列表」勾选想隐藏的应用，点右下角按钮确认
3. 「已隐藏」页可单个恢复或全部恢复
4. 「设置」里可切换模式、开启生物识别、管理桌面数据库配置
5. 开启「启动时验证」后，每次冷启动 App 都需要指纹 / 面部 / 设备密码

**本工具会自动排除自身**，不会让你把自己藏起来导致无法打开。

### 应急恢复

如果因为误操作（例如通过 adb 禁用了关键应用）导致设备异常，可以在电脑上使用：

```bash
adb shell pm list packages -d            # 查看被禁用的包
adb shell pm enable <包名>               # 恢复指定包
```

---

## 安全设计

- 所有拼接进 shell 命令的包名都经过白名单正则校验（`^[A-Za-z][A-Za-z0-9_.]*$`），阻断命令注入
- SQL 整段通过 POSIX 单引号转义后传入 `sqlite3`，`{pkg}` 只会被替换成合法包名
- 隐藏记录与生物识别开关不参与云备份 / 设备迁移
- 生物识别使用 `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`，无生物识别的设备可回退到设备密码

## 风险提示

- 禁用系统关键应用（如 SystemUI、设置、桌面本身）可能导致设备无法正常使用的「软砖」，恢复通常需要 adb 或 recovery
- 桌面数据库模式需要直接写桌面 App 的私有数据，错误的 SQL 可能破坏桌面配置，操作前请备份
- root 权限意味着本工具的能力边界很高，请只从可信来源获取本项目的构建产物

---

## 项目结构

```
AppCloak/
├── app/src/main/java/com/appcloak/
│   ├── MainActivity.kt                 入口，管理解锁状态
│   ├── AppCloakApp.kt
│   ├── core/
│   │   ├── RootManager.kt              su 执行、包名校验、shell 转义
│   │   └── ShellResult.kt
│   ├── data/
│   │   ├── AppInfo.kt / HiddenRecord.kt / HideMode.kt
│   │   ├── AppRepository.kt            读取已安装应用
│   │   ├── LauncherDbConfig.kt         桌面数据库配置模型
│   │   └── PreferencesStore.kt         本地持久化
│   ├── hide/
│   │   ├── HideStrategy.kt             策略接口
│   │   ├── PmDisableStrategy.kt        pm 禁用方案
│   │   └── LauncherDbStrategy.kt       桌面数据库方案（扩展入口）
│   └── ui/                             Compose 界面与 MainViewModel
└── tools/generate_icons.py             生成 mipmap 图标（纯标准库）
```
