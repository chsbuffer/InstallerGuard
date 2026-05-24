# Installer Guard / 安装器锁定器

[![GitHub](https://img.shields.io/badge/chsbuffer/InstallerGuard-181717?logo=github)](https://github.com/chsbuffer/InstallerGuard)
[![Xposed](https://img.shields.io/badge/libxposed-101-blueviolet)](app/src/main/resources/META-INF/xposed/module.prop)

---

### Introduction / 项目简介

A LSPosed module that forces all APK installation requests on Android to be handled by a user-specified package installer. By default, it redirects to [vvb2060's Shizuku Package Installer](https://github.com/vvb2060/PackageInstaller).

一个 LSPosed 模块，强制将 Android 系统上的所有 APK 安装请求交由用户指定的安装器处理。默认重定向至 [vvb2060 的 Shizuku 安装器](https://github.com/vvb2060/PackageInstaller)。

### Features

- Real-time toggle — enable or disable interception without reboot
- Dynamically switch installer app — changes take effect immediately

> **Note:** A reboot is required after initially activating the module in LSPosed, and after updating the module. Once loaded, the toggle and installer switch work in real time.

### 功能特性

- 实时开关，启用/禁用拦截无需重启 
- 动态切换安装器应用，修改即时生效 

> **注意：** 模块首次在 LSPosed 中激活后需要重启，模块更新后也需要重启。加载成功后，开关和切换安装器可实时生效。

---

### License / 许可

Apache License 2.0

Copyright © ChsBuffer