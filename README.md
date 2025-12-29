# 762Map

## 主页

### 地图

1. 移动：上下左右按钮/可拖拽
2. 缩放：+-按钮/手势
3. 一键回到当前位置：按钮
4. 定位与导航：显示最短路线
5. 搜索：按钮，点击后跳转搜索界面

可选

1. 指南针：显示方向
2. 交通方式

## 搜索界面

搜索，显示地点

可选

1. 显示地点及直线距离
2. 历史记录

## 登陆注册界面

先套一个注册登录界面

多用户

1. 普通用户
2. 游客
3. VIP：开放可选功能
   1. 管理员

账户：手机号格式

## 我的

### 数据面板

1. 历史里程
2. 点亮城市

### 账户信息

1. 昵称/头像显示
2. 手机号
3. 退出登录
4. 修改密码

可选

1. 绑定邮箱

# 762Map 开发环境

- 编程语言：Kotlin
- Gradle版本：gradle-8.13

- jdk版本：jdk-17

- Sdk版本：

  ```
  android {
      namespace = "com.example.a762map"
      compileSdk {
          version = release(36)
      }
  
      defaultConfig {
          applicationId = "com.example.a762map"
          minSdk = 30
          targetSdk = 36
          versionCode = 1
          versionName = "1.0"
  
          testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      }
  ```

- 其他信息：

  ```
  package com.example.a762map
  ```

  
