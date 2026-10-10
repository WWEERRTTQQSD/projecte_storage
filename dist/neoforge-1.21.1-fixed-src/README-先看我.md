# NeoForge 1.21.1 修复版源码（r12-neoforge-1.21.1）

本工程是 projecte_storage_mod_emc 的 NeoForge 1.21.1 代码线（在 r8 基础上修复）。
r11 起整棵源码树用真实 NeoForge 21.1 工具链干净重编译，取代旧的"单类热修塞回 r8 jar"做法
（旧热修包已归档到 dist/archive/，请勿安装）。
现行成品：`dist/projecte_storage_mod_emc-1.0.0-neoforge-1.21.1-r12.jar`
文件名认不清就记住：**1.21.1 手机实例装带 "neoforge-1.21.1" 的那只**；
带 "forge-1.20.1" 的是电脑端 1.20.1 实例（Forge / NeoForge 47.x 通用）专用，两者不能互装。

## r12 改动（前置声明）

- `META-INF/neoforge.mods.toml`：ProjectE（等价交换）依赖由 `type="optional"` 改为
  `type="required"`，`ordering="AFTER"` 不变，版本范围仍是 `[0,)`。
  即：**等价交换是本模组的硬前置**，未安装或版本对不上时 NeoForge 会在加载阶段直接报
  缺失必需依赖并拒绝启动；Mod 列表里也会把 ProjectE 显示为本模组的前置。

## 相对 r8 的改动（修复开档崩溃）
- EmcLinkConfig.java：两个字符串键 extractionMode / nbtFilterMode 从 defineInList
  改为无校验器的 define。defineInList 生成的校验器对不可变列表直接 contains(value)，
  旧配置文件缺该键时以 null 调用，List.of(...).contains(null) 抛 NullPointerException，
  FML 报 "Failed to wait for future Config loading, 1 errors found" 后开档崩溃（r8 日志实锤）。
  取值合法性移到读取端做忽略大小写的宽容匹配，陌生值自动回退默认值。
- EmcNbtFilter.java：读取端归一化，null/空/陌生值按默认 ANY_NBT。
- extractionEnabled()：null/陌生值按默认 ENABLED（保证终端一键合成可用）。

## 编译方法
由平台编译器（mc_build）在本目录构建，使用真实依赖 net.neoforged:neoforge:21.1.244。
产物：build/libs/projecte_storage_mod_emc-1.0.0-neoforge-1.21.1-r12.jar
（同内容副本已放在工程 dist/ 顶层供下载。r11 旧 jar 请勿再装：它的前置是可选的。）

## 注意
- 只适用于 Minecraft 1.21.1 + NeoForge 21.1.x（实测过的加载器：21.1.256）。
- 若之前跑过旧版本，建议删除存档目录下 config/projecte_storage_mod_emc-common.toml
  后首次启动，让模组重新生成干净的配置（新代码已能容忍旧文件，这一步只是保险）。
