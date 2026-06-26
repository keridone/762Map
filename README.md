![762map_logo](762map_logo.png)

762Map 是一款基于 Android + Kotlin 开发的移动地图应用，围绕“地图定位、地点搜索、路线导航、地点打卡、个人足迹与会员服务”构建。项目接入高德地图 SDK，支持 POI 搜索联想、当前位置定位、驾车路线绘制，并提供本地用户体系、VIP 权益、管理员用户管理、地点照片打卡和年度出行数据展示等功能。

## 功能特性

- **地图定位与交互**
  - 高德地图展示、缩放、指南针、比例尺与当前位置按钮
  - 获取当前定位并自动移动到用户所在位置
  - 地图 Marker 点击后进入地点详情页

- **地点搜索与路线导航**
  - 基于高德 Inputtips 的搜索联想
  - POI 兜底检索，支持将搜索结果定位到地图
  - 从当前位置到目标地点绘制驾车路线
  - 支持清除当前导航路线

- **地点详情与照片打卡**
  - 展示地点名称与经纬度信息
  - 支持从相册选择图片或调用相机拍照
  - 使用 FileProvider 保存并授权访问打卡图片
  - 基于 SQLite 按地点保存打卡照片记录

- **用户与会员体系**
  - 支持手机号/邮箱登录、注册与会话保持
  - 普通用户、VIP 用户、管理员三类角色
  - 用户信息编辑、头像字段、本地账户数据管理
  - 普通用户可升级为 VIP

- **个人中心与出行数据**
  - 展示总里程、年度里程、导航次数等统计数据
  - 足迹、年度报告、PDF 导出等页面入口
  - VIP 用户支持一键救援入口

- **管理员管理**
  - 管理员可进入用户管理页面
  - 支持按角色查看、新增、编辑、删除用户
  - 支持维护用户里程、导航次数、点亮城市等统计数据

## 技术栈

- **开发语言**：Kotlin
- **平台**：Android
- **构建工具**：Gradle / Android Gradle Plugin
- **最低系统版本**：Android 11（minSdk 30）
- **目标 SDK**：Android API 36
- **地图能力**：高德地图、定位、搜索、路线规划 SDK
- **UI 组件**：AndroidX、Material Components、Navigation、ViewBinding、RecyclerView、CardView
- **数据存储**：SQLiteOpenHelper、本地 SharedPreferences 会话
- **文件访问**：Android FileProvider
- **邮件能力**：JavaMail Android，用于 VIP 一键救援邮件发送

## 项目结构

```text
762Map/
├── app/
│   ├── src/main/java/com/example/a762map/
│   │   ├── data/
│   │   │   ├── checkin/        # 地点照片打卡数据、图片存储、SQLite 表结构
│   │   │   └── search/         # 高德搜索、POI、路线规划封装
│   │   ├── ui/
│   │   │   ├── map/            # 地图页、搜索联想、导航请求
│   │   │   ├── place/          # 地点详情与打卡照片
│   │   │   └── profile/        # 登录注册、个人中心、VIP、管理员、报告页面
│   │   └── A762MapApp.kt
│   ├── src/main/res/
│   │   ├── layout/             # Activity / Fragment 布局
│   │   ├── drawable/           # 图标与背景资源
│   │   ├── menu/               # 底部导航菜单
│   │   └── navigation/         # Navigation Graph
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 运行环境

请确保本地环境满足以下条件：

- Android Studio Narwhal 或更高版本
- JDK 17 或 Android Studio 自带 JDK
- Android SDK Platform 36
- 可用的 Android 模拟器或真机
- 高德开放平台 Android Key

## 快速开始

1. 克隆项目：

   ```bash
   git clone https://github.com/yourname/762Map.git
   cd 762Map
   ```

2. 使用 Android Studio 打开项目，等待 Gradle Sync 完成。

3. 配置高德地图 Key。

   当前项目在 `app/build.gradle.kts` 中通过 `manifestPlaceholders["AMAP_API_KEY"]` 注入高德 Key。正式使用时建议替换为自己的 Key，并避免将真实密钥提交到公开仓库。

   ```kotlin
   manifestPlaceholders["AMAP_API_KEY"] = "你的高德地图 Key"
   ```

4. 运行项目：

   ```bash
   ./gradlew assembleDebug
   ```

   Windows 可使用：

   ```bash
   .\gradlew.bat assembleDebug
   ```

5. 在 Android Studio 中选择模拟器或真机，点击 Run 启动应用。

## 权限说明

应用运行时会用到以下 Android 权限：

- `INTERNET`：访问高德地图、搜索、路线规划等网络服务
- `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE`：网络状态判断
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`：当前位置定位与导航起点获取
- `CAMERA`：地点拍照打卡

## 示例账户

项目初始化 SQLite 数据库时会内置若干演示用户，方便调试登录、VIP 和管理员功能：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `15111111111` 或 `admin@a762map.com` | `123456` |
| 普通用户 | `18500000001` 或 `u1@test.com` | `111111` |
| VIP 用户 | `18500000003` 或 `vip1@test.com` | `111111` |

## 注意事项

- 高德地图 SDK 需要在开放平台配置正确的包名、SHA1 和 Key，否则地图、定位或搜索可能无法正常工作。
- VIP 一键救援邮件功能依赖 SMTP 配置。公开仓库中请勿提交真实邮箱、授权码或其他敏感信息，建议改为本地配置、环境变量或后端接口下发。
- PDF 导出页面目前保留了入口和页面结构，后续可继续接入真实 PDF 生成逻辑。
- 项目当前使用本地 SQLite 存储用户和打卡数据，适合课程设计、原型验证和离线 Demo；如需生产使用，建议接入后端认证、云端数据同步和更完善的权限控制。

## 后续优化方向

- 接入真实后端服务，实现用户数据云端同步
- 完善年度报告与 PDF 导出
- 增加真实导航过程中的里程统计与轨迹记录
- 优化地点详情页信息展示与图片预览体验
- 将敏感配置迁移到安全配置文件或服务端
- 增加单元测试、UI 测试和 CI 构建流程

## License

本项目基于 [MIT License](LICENSE) 开源。
