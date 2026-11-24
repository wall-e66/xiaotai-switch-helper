# 小泰换号助手（com.dsh.xtclean）

## 解决什么问题

「小泰智维」点 **安全退出** 后，再点 **正泰内部登录** 会自动登回原来那个账号，无法登录其他账号。

原因：安全退出只清掉了 App 的业务 token，**没有清正泰统一身份认证（SSO）的本地凭证**（WebView cookie / 本地缓存）。所以一进「内部登录」页面，SSO 直接把你认回原账号。

## 怎么解决

一键清掉小泰智维的本地数据（等价于 设置 → 应用 → 小泰智维 → 存储 → 清除数据），SSO 凭证一起清掉，App 回到干净的登录页，就能输入新账号登录。

本 App 通过 **Shizuku** 以 shell 身份执行 `pm clear com.chint.inspect.mvptest`，不需要 root。

## 使用步骤

1. 手机上装好并启动 **Shizuku**（开发者选项 → 无线调试 / ADB 启动）。
2. 安装本 App 的 `app-debug.apk`。
3. 打开 App → 点「一键清理并打开小泰智维」→ 第一次会弹 Shizuku 授权，点允许。
4. 清理完成后小泰智维自动打开，输入新账号密码或手机验证码登录即可。

没装 Shizuku 也能用：点「应用信息（手动清除数据）」→ 存储 → 清除数据。

## 在线编译

推送到 main 分支或手动点 **Actions → build-apk → Run workflow**，跑完在 job 的 Artifacts 里下载 `xtclean-apk`。

## 目录

```
app/src/main/java/com/dsh/xtclean/MainActivity.java   全部逻辑
app/src/main/AndroidManifest.xml                      Shizuku provider
.github/workflows/build.yml                           GitHub Actions 打包
```
