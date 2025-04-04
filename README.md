# 📱 屏幕共享应用 (ScreenSharing)

## 📝 项目简介

这是一个轻量级的Android屏幕共享应用，允许用户将手机屏幕实时投射到局域网内的其他设备上。应用使用H.265/HEVC编码技术以高效率压缩视频数据，并通过WebSocket传输，确保低延迟的屏幕共享体验。

## ✨ 主要功能

- 📲 实时屏幕投射到局域网内的其他设备
- 🔄 高效H.265/HEVC视频编码(自动降级到H.264)
- 📶 通过WebSocket实现低延迟数据传输
- 🧩 简洁直观的用户界面
- 🔍 自动检测并显示本机IP地址

## 🛠️ 技术架构

项目采用了以下技术实现屏幕共享功能：

1. **媒体投射API**: 使用Android的`MediaProjection`API录制屏幕
2. **视频编码**: 使用`MediaCodec`进行H.265/HEVC编码
3. **网络传输**: 通过WebSocket协议传输编码后的视频流
4. **权限管理**: 动态请求必要的系统权限

## 📋 使用方法

1. 📲 在Android设备上安装应用
2. 🚀 启动应用并点击"开始投射"按钮
3. ✅ 授予必要的屏幕录制权限
4. 📡 应用将显示本机IP地址和端口号
5. 💻 在接收端设备上连接到该IP地址和端口(9837)
6. 👀 开始观看共享的屏幕内容

## 🔒 隐私与权限

应用需要以下权限才能正常工作：

- `INTERNET`: 用于网络通信
- `RECORD_AUDIO`: 屏幕录制通常需要此权限
- `FOREGROUND_SERVICE`: 用于后台运行服务

## 💡 技术细节

- 🎬 屏幕录制分辨率: 720x1280
- 🎞️ 视频编码: H.265/HEVC (自动降级到H.264)
- 🖼️ 帧率: 20fps
- 🔌 默认端口: 9837

## 🔄 未来计划

- [ ] 增加视频画质设置选项
- [ ] 支持音频传输
- [ ] 开发Web客户端接收器
- [ ] 添加安全认证机制
- [ ] 优化网络传输性能

## ✉️ 联系方式

如有问题或建议，欢迎通过以下邮箱联系开发者：
📧 example@example.com 