这些 jar 都已过期，仅作溯源，不要安装：
  - *-r8.jar / *-r9.jar：1.21.1 旧线，r8 开档崩溃（config NPE）。
  - *-r10-forge-r10.jar：1.20.1 Forge 通用线旧命名版。
  - *-r10-neoforge-1.21.1.jar：r8 的单类热修包，已被整树重编的 r11 取代。
  - *-neoforge-1.21.1-r11.jar：NeoForge 1.21.1 旧版，ProjectE 前置还是「可选」，
    已被前置改成 required 的 r13 取代。

现行成品（r13，两边前置都已是硬依赖「等价交换 ProjectE」）：
  1.20.1（Forge 与 NeoForge 47.x 通用）：dist/projecte_storage_mod_emc-1.0.0-forge-1.20.1-r13.jar
  1.21.1（NeoForge 21.1.x）：dist/projecte_storage_mod_emc-1.0.0-neoforge-1.21.1-r13.jar
  两只打包下载：dist/projecte_storage_mod_emc-r13-双版本.zip
