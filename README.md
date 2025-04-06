# 📱 屏幕分享应用 (ScreenSharing)

## 📋 项目简介

ScreenSharing是一个Android设备间的屏幕投射/分享应用，通过WebSocket实现设备间的实时屏幕共享。一台设备可以将屏幕内容编码并传输给另一台设备进行解码和显示，非常适合演示、教学和远程协助等场景。

## ✨ 主要功能

- 🔄 实时屏幕共享：将一台Android设备的屏幕内容实时传输到另一台设备
- 📽️ 高效视频编码：使用H.265/HEVC编码技术，支持H.264/AVC回退
- 🌐 WebSocket传输：通过WebSocket协议实现稳定的网络传输
- 📱 自适应显示：自动适应不同设备的屏幕尺寸和比例
- 🔒 权限管理：符合Android最新的权限管理规范

## 📲 使用方法

1. **在发送端设备上**：
   - 打开Share11或Share14应用
   - 点击"开始"按钮并授予屏幕录制权限
   - 应用将显示连接地址（IP地址和端口号）

2. **在接收端设备上**：
   - 打开View应用
   - 在主界面输入发送端设备的IP地址和端口号（格式：`IP地址:端口号`）
   - 点击"显示"按钮开始接收和显示远程屏幕

## 🛠️ 安装方法

### 方法一：直接安装APK

1. 下载Share11/Share14和View应用的APK文件
2. 在Android设备上安装APK
3. 授予必要的权限

### 方法二：从源码构建

1. 克隆本仓库到本地：
   ```bash
   git clone https://github.com/yourusername/ScreenSharing.git
   ```
2. 使用Android Studio打开项目
3. 分别构建Share11/Share14和View模块
4. 将生成的APK安装到设备上

## 📁 项目结构

```
ScreenSharing/
├── View/                       # 接收端应用模块
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/yuer/view/
│   │   │   │   ├── MainActivity.java     # 主界面，处理用户输入
│   │   │   │   ├── SecondMain.java       # 视频显示界面，处理解码和显示
│   │   │   │   └── SocketServer.java     # WebSocket客户端实现
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml    # 主界面布局
│   │   │   │   │   └── activity_second.xml  # 视频显示界面布局
│   │   │   │   └── ...                   # 其他资源文件
│   │   │   └── AndroidManifest.xml       # 应用清单文件
│   │   └── build.gradle                  # 模块构建配置
│   └── ...                               # 其他项目文件
│
├── Share11/                     # 发送端应用模块（Android 11+）
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/yuer/share11/
│   │   │   │   ├── MainActivity.java     # 主界面和权限管理
│   │   │   │   ├── PermissionUtil.java   # 权限工具类
│   │   │   │   ├── encode/
│   │   │   │   │   └── CodecH265.java    # H.265视频编码实现
│   │   │   │   └── socket/
│   │   │   │       ├── SocketServer.java # WebSocket服务器
│   │   │   │       └── SocketService.java # 屏幕录制和传输服务
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   └── ...
│
└── Share14/                     # 发送端应用模块（Android 14+）
    ├── app/
    │   ├── src/main/
    │   │   ├── java/com/yuer/share14/
    │   │   │   ├── ... # 类似Share11，但适配Android 14
    │   │   ├── res/
    │   │   └── AndroidManifest.xml
    │   └── build.gradle
    └── ...
```

## 💻 技术栈

- **编程语言**：Java
- **媒体处理**：
  - Android MediaCodec API
  - H.265/HEVC 和 H.264/AVC 视频编解码
- **网络通信**：
  - WebSocket (使用java-websocket库)
  - JSON数据交换
- **UI组件**：
  - SurfaceView（视频显示）
  - Android原生UI控件

## 👤 关于作者

**欲儿 (Yuer)**

专注于Android开发和多媒体处理的开发者，热衷于创建实用且高效的工具应用。

## 📝 许可证

本项目采用MIT许可证，详情请查看LICENSE文件。

## 🙏 特别致谢

感谢所有为本项目提供测试和反馈的用户。 