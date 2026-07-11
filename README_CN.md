<div align="center">
<img alt="LOGO" src="desktopApp/resources/icon.png" width="256" height="256" />

# FormulaDock

**基于 Kotlin Multiplatform 与 Compose Multiplatform 构建的跨平台公式定义、管理与计算工具**

[![GitHub Release](https://img.shields.io/github/v/release/shdmfire/FormulaDock?style=flat-square&label=Latest)](https://github.com/shdmfire/FormulaDock/releases/latest)
[![License](https://img.shields.io/github/license/shdmfire/FormulaDock?style=flat-square)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/shdmfire/FormulaDock?style=flat-square)](https://github.com/shdmfire/FormulaDock/stargazers)
[![GitHub Downloads](https://img.shields.io/github/downloads/shdmfire/FormulaDock/total?style=flat-square&label=Downloads)](https://github.com/shdmfire/FormulaDock/releases)

[下载](https://github.com/shdmfire/FormulaDock/releases/latest) · [问题反馈](https://github.com/shdmfire/FormulaDock/issues)

**[English](README.md)** | **中文**

</div>

---

<p align="center">
  <img src="images/formula_list.jpg" width="23%" alt="公式列表" />
  <img src="images/quick_calcuator_panel.jpg" width="23%" alt="计算面板" />
  <img src="images/expression.jpg" width="23%" alt="表达式编辑" />
  <img src="images/single_formula_export_import_share.jpg" width="23%" alt="导出与分享" />
</p>
<p align="center">
  <img src="images/formula_data_management_export.jpg" width="23%" alt="数据导出" />
  <img src="images/formula_data_management_import.jpg" width="23%" alt="数据导入" />
  <img src="images/setting.jpg" width="23%" alt="设置" />
  <img src="images/notification.jpg" width="23%" alt="通知" />
</p>

# FormulaDock

FormulaDock 是一个基于 **Kotlin Multiplatform (KMP)** 和 **Compose Multiplatform** 构建的跨平台公式定义、管理与计算工具。它支持在 Android 和桌面端（Windows、Linux）流畅运行，帮助用户自由构建与运行各种数学及业务公式。

---

## 📖 项目介绍

### 解决什么问题？
在日常生活和工作中，我们经常需要处理各种重复的、多步骤的计算流程（例如：旅行费用分摊、房间刷漆预算、自由职业报价、家庭账单预估等）。使用常规的计算器需要反复手动输入和计算中间步骤，容易出错且效率低下；而使用 Excel 等电子表格软件又显得过于臃肿，不便于在移动设备上快速输入和查看。

**FormulaDock 完美解决了这一痛点**，带来如下核心设计：
1. **即开即用与快速唤醒（头号特性）**：在 Android 端可通过**系统下拉通知栏的通知中心**一键启动，在 Windows 端可通过自定义**全局热键**在后台瞬间唤醒或隐藏**快速计算面板**。
2. **一次性定义输入项**：指定自定义 Key、显示名称、默认值、单位、是否必填以及排序。
3. **配置固定常量**：定义固定参数（如税率或换算系数）。
4. **定义输出项**：使用数学表达式计算结果，并指定保留小数精度与单位。
5. **实时动态运算**：输入数据时，应用将实时进行运算并展现结果，且会自动将计算数据保存到本地的**历史记录**中。

---

### 主要功能

1. **自定义公式编辑器**
   - 支持自由创建与编辑公式，定义公式名称及描述。
   - 动态添加/删除/编辑**输入项（Inputs）**：可设置变量 Key、显示名称（Label）、默认值、单位、是否必填以及排序。
   - 动态添加/删除/编辑**常量（Constants）**：可设置常量 Key、固定值、单位。
   - 动态添加/删除/编辑**输出项（Outputs）**：支持编写数学计算表达式、设置保留小数位数（精度）、单位及排序。

2. **多功能计算面板 (Formula Run)**
   - 根据公式定义的输入参数，自动生成表单界面。
   - 实时进行表达式求值（在输入框内容变化时，自动刷新所有输出计算结果）。
   - 包含常用的内置公式模板（如路程开销规划、房屋刷漆预算、自由职业项目报价、每月家庭预算、家庭电费估算等）。

3. **计算历史记录 (Calculation History)**
   - 自动保存每次计算的输入参数与输出结果。
   - 支持按时间查看历史计算详情，便于数据回溯。

4. **跨平台导入与导出 (Formula I/O)**
   - 支持通过标准的系统文件选择器（FileKit）将自定义公式导出为 JSON 文件，或从 JSON 文件导入公式。

5. **系统深度整合与快速唤醒**
   - **Android 整合**：支持通过**下拉系统通知栏的通知中心**快速启动并打开**快速计算面板 (quick_calculator_panel)**。
   - **Windows 整合**：支持在 Windows 系统中注册**全局热键**，即使程序在后台运行，也能通过热键快速唤醒/启动或隐藏**快速计算面板 (quick_calculator_panel)**。
   - **系统托盘**：桌面端支持原生系统托盘（System Tray）操作。

6. **偏好设置与多语言**
   - 支持深色/浅色模式切换。
   - 支持多语言（i18n）自适应。

---

### 支持的平台

| 平台 | 支持情况 | 最低/推荐版本要求 |
| :--- | :--- | :--- |
| **Android** | 🟢 已支持 | 最低支持 **Android 7.0** (API 24) / 编译目标 API 37 |
| **Windows** | 🟢 已支持 | 支持 Windows 10/11，提供 `.exe` 和 `.msi` 安装包 |
| **Linux** | 🟡 未测试 | 支持常见 Linux 发行版，提供 `.deb` 安装包（暂未在物理设备上进行实际测试） |
| **JVM/JDK** | 🟢 已支持 | 需要 **JDK 11** 或更高版本运行环境 |

---

## 🛠 技术栈

项目只列出实际使用的核心技术和第三方库：

### 核心框架
* **Kotlin Multiplatform (KMP)** - 跨平台核心逻辑与数据共享。
* **Compose Multiplatform (Jetbrains)** - 跨平台 UI 声明式框架（共享 Android & Desktop 视图界面）。
* **Gradle (Kotlin DSL)** - 项目构建与依赖管理。

### 数据存储 & 持久化
* **SQLDelight (2.3.2)** - 跨平台 SQLite 数据库生成与类型安全查询。
* **Multiplatform Settings (1.3.0)** - 跨平台 Key-Value 配置存储（用于保存主题偏好等设置）。

### 业务与计算引擎
* **Multiplatform Expressions Evaluator (2.0.0)** - 跨平台数学表达式求值解析引擎（用于实时计算公式）。
* **Kotlinx Coroutines (1.11.0)** - 协程异步编程与线程调度。
* **Kotlinx Serialization JSON (1.11.0)** - 跨平台 JSON 序列化（用于公式导入/导出）。
* **Kotlinx Datetime (0.8.0)** - 跨平台时间日期处理。

### 平台整合 & UI 辅助
* **FileKit (0.14.2)** - 跨平台文件选择与沙盒对话框。
* **Compose Native Tray (1.3.3)** - 桌面端系统托盘集成。
* **Nucleus Global Hotkey (1.15.7)** - 桌面端全局快捷键绑定库。
* **Android Jetpack Component (Lifecycle & Navigation3)** - 状态生命周期管理与现代路由导航。

---

## 🚀 快速开始

### 运行应用程序

您可以使用 IDE 的运行配置或通过命令行运行：

* **Android 应用程序**:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```

* **桌面端应用程序 (Desktop)**:
  * **热重载模式 (Hot reload)**:
    ```bash
    ./gradlew :desktopApp:hotRun --auto
    ```
  * **标准运行**:
    ```bash
    ./gradlew :desktopApp:run
    ```

### 运行测试

* **Android 测试**:
  ```bash
  ./gradlew :shared:testAndroidHostTest
  ```
* **桌面端测试**:
  ```bash
  ./gradlew :shared:jvmTest
  ```

---

## 🙏 致谢

FormulaDock 的开发离不开优秀的开源项目和开源社区。特别感谢以下项目的作者与贡献者：

| 项目                                                                                                       |           当前使用版本          | 在 FormulaDock 中的用途                                            |
| :------------------------------------------------------------------------------------------------------- | :-----------------------: | :------------------------------------------------------------ |
| [Kotlin](https://github.com/JetBrains/kotlin)                                                            |           2.4.0           | 项目使用的主要编程语言、标准库以及多平台编译工具链                                     |
| [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)                              |           1.11.1          | Android 与桌面端共享的声明式用户界面框架                                      |
| [Compose Multiplatform Core](https://github.com/JetBrains/compose-multiplatform-core)                    | Material 3 1.11.0-alpha07 | Material 3 组件、Compose UI、Foundation、Runtime 及扩展图标             |
| [AndroidX / Android Jetpack](https://github.com/androidx/androidx)                                       |            多个版本           | Activity、AppCompat、Core、Lifecycle、Navigation 3 与 Android 测试组件 |
| [SQLDelight](https://github.com/sqldelight/sqldelight)                                                   |           2.3.2           | 跨平台 SQLite 数据库、类型安全查询和数据库驱动                                   |
| [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)                            |           1.3.0           | 跨平台 Key-Value 偏好设置和协程扩展                                       |
| [Multiplatform Expressions Evaluator](https://github.com/murzagalin/multiplatform-expressions-evaluator) |           2.0.0           | 数学表达式解析与公式求值                                                  |
| [KotlinX Coroutines](https://github.com/Kotlin/kotlinx.coroutines)                                       |           1.11.0          | 异步任务、协程调度和响应式状态处理                                             |
| [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization)                                 |           1.11.0          | 公式数据的 JSON 序列化、导入与导出                                          |
| [KotlinX DateTime](https://github.com/Kotlin/kotlinx-datetime)                                           |           0.8.0           | 跨平台日期与时间处理                                                    |
| [FileKit](https://github.com/vinceglb/FileKit)                                                           |           0.14.2          | 跨平台文件选择、公式导入和公式导出                                             |
| [Compose Native Tray](https://github.com/kdroidFilter/ComposeNativeTray)                                 |           1.3.3           | Windows、macOS 和 Linux 的原生系统托盘集成                               |
| [Nucleus Global Hotkey](https://github.com/NucleusFramework/Nucleus/tree/main/global-hotkey)             |           1.15.7          | 桌面端全局快捷键注册与处理                                                 |
| [SLF4J](https://github.com/qos-ch/slf4j)                                                                 |           2.0.18          | JVM 日志门面；项目使用 NOP 实现关闭默认日志输出                                  |
| [JUnit 4](https://github.com/junit-team/junit4)                                                          |           4.13.2          | JVM 与 Android 单元测试支持                                          |

同时感谢 JetBrains、Google、Android Open Source Project，以及所有参与上述项目维护、测试、文档编写和问题反馈的贡献者。

上述项目的版权归各自的作者和贡献者所有，并分别遵循各自的开源许可证。FormulaDock 使用 Apache License 2.0 发布，并不改变或取代第三方项目原有的许可证和版权声明。

---

## 📄 许可证

```text
Copyright 2026 [Copyright Holder]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License in the LICENSE file included
with this repository.
```

FormulaDock 基于 **Apache License 2.0** 开源。

你可以在遵守许可证条款的前提下使用、复制、修改和分发本项目。许可证全文请参阅仓库根目录中的 [`LICENSE`](LICENSE) 文件。

除非适用法律要求或另有书面约定，本项目按“原样”提供，不附带任何明示或默示的担保。详细条款以英文版 Apache License 2.0 许可证全文为准。

项目中使用的第三方库、字体、图标及其他资源仍分别遵循其原始许可证和版权声明。