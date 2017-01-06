# UCloud 直播云 SDK 文档

UCDLive SDK 是由 UCloud 提供的低延时、高并发的直播云服务。

![screenshot-1](screenshot/screenshot-1.png)  
![screenshot-2](screenshot/screenshot-2.png)  
![screenshot-3](screenshot/screenshot-3.png)  
![screenshot-4](screenshot/screenshot-4.png)  

## 功能特性

- 支持音视频硬编、软编(H.264 & AAC)
- 支持 RTMP 封包 & 推流
- 支持5种清晰度切换
    1. 自动 (根据屏幕大小自动设定)
    2. 640 * 480 (4:3)
    3. 320 * 180 (16:9 Low)
    4. 640 * 360 (16:9 Normal)
    5. 1280 * 720 (16:9 High)
- 支持前、后置摄像头动态切换
- 支持推流横、竖屏动态切换
- 支持滤镜模式(CPU & GPU)动态切换
- 支持视频镜像、非镜像操作
- 支持静音操作
- 支持闪关灯操作
- 支持自动对焦
- 支持截帧操作
- 支持美颜滤镜(磨皮、美白、红润)，可设置5种效果等级
- 支持动态贴纸(Faceu等)
- 支持自定义滤镜，兼容GPUImage
- 支持音视频源数据回调，可自定义数据处理
- 支持 arm、armv7a、arm64-v8a、x86 主流芯片体系架构
- 支持 RTMP 推流地址鉴权功能
- 支持云适配功能
- 支持直播鉴黄功能
- 支持混音效果
- 支持 ip 推流

## 版本要求

Android 4.3+ (API 18+)

## 使用方法

请参考 wiki 文档：[UCDLive_Android 开发指南][1]

<a name="6"></a>
## 播放器集成
详细请参考[UCDMediaPlayer项目][2]。

## 反馈和建议
  - 主 页：<https://www.ucloud.cn/>
  - issue：[查看已有 issues 和提交 Bug 推荐][3]
  - 邮 箱：[sdk_spt@ucloud.cn][4]

### 问题反馈参考模板

|名称|描述|
|---|---|
|设备型号|华为 Mate 8|
|系统版本|Android 5.0|
|SDK 版本|v1.4.1|
|问题描述|描述问题现象|
|操作路径|重现问题的操作步骤|
|附件|推流界面截屏、推流调试日志截屏、报错日志截图等|

[1]: https://github.com/umdk/UCDLive_Android/wiki
[2]: https://github.com/umdk/UCDMediaPlayer_Android
[3]: https://github.com/umdk/UCDLive_Android/issues
[4]: mailto:sdk_spt@ucloud.cn
