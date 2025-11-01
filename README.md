# AI-Precise 图像标注系统

[![Version](https://img.shields.io/badge/version-1.0.1-blue.svg)](https://github.com/xxh123001/AI-Precise)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

## 📋 项目简介

AI-Precise 是一个专业的医学图像标注系统，专为肾小管图像的精确标注而设计。系统支持多用户协作标注、标注一致性分析、数据导出等功能。

### 🌟 主要特性

- ✅ **多用户协作标注** - 支持多位专家对同一图像进行标注
- 📊 **标注一致性分析** - 自动计算标注一致性，生成详细报告
- 🎯 **任务分配管理** - 灵活的任务分配和进度跟踪
- 📈 **数据统计分析** - 全面的标注数据统计和可视化
- 💾 **数据导出功能** - 支持多种格式的数据导出
- 🔐 **权限管理** - 完善的用户角色和权限控制

### 🏥 应用场景

- 肾小管类型识别（PT、DT、CD、TAL 等）
- 萎缩程度判断
- 医学图像质量评估
- 多专家协同标注

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **认证**: JWT
- **语言**: Java 17

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **UI 组件**: Element Plus
- **状态管理**: Pinia
- **HTTP 客户端**: Axios

### 数据分析
- **语言**: Python 3.10
- **数据处理**: Pandas, NumPy
- **数据库连接**: mysql-connector-python

## 📦 项目结构

```
AI-Precise/
├── client/                 # 前端项目
│   ├── src/
│   │   ├── api/           # API 接口
│   │   ├── views/         # 页面组件
│   │   ├── router/        # 路由配置
│   │   └── store/         # 状态管理
│   └── package.json
│
├── tag_backend/           # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/tag_backend/
│   │   │   │       ├── controller/    # 控制器
│   │   │   │       ├── service/       # 业务逻辑
│   │   │   │       ├── entity/        # 实体类
│   │   │   │       ├── repository/    # 数据访问
│   │   │   │       └── security/      # 安全配置
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
│
└── .gitignore            # Git 忽略配置
```

## 🚀 快速开始

### 环境要求

- **JDK**: 17 或更高版本
- **Node.js**: 16 或更高版本
- **MySQL**: 8.0 或更高版本
- **Redis**: 6.0 或更高版本
- **Python**: 3.10 或更高版本（用于数据分析）

### 后端启动

```bash
cd tag_backend

# 配置数据库
# 编辑 src/main/resources/application.properties
# 设置数据库连接信息

# 启动后端服务
./mvnw spring-boot:run

# 或使用快速重启脚本
./restart_backend.sh
```

### 前端启动

```bash
cd client

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

### 数据库初始化

系统首次启动时会自动创建数据库表和初始数据。

默认管理员账号：
- 用户名: `admin`
- 密码: `admin123`

## 📊 功能模块

### 1. 用户管理
- 用户注册与登录
- 角色权限管理
- 用户任务分配

### 2. 图像管理
- 图像上传与存储
- 图像预处理
- 图像裁剪功能

### 3. 标注功能
- 交互式标注界面
- 多标签标注
- 标注历史记录

### 4. 任务管理
- 任务创建与分配
- 任务进度跟踪
- 任务完成统计

### 5. 数据分析
- 标注一致性分析
- 标注质量评估
- 多维度数据统计

### 6. 数据导出
- CSV 格式导出
- Excel 格式导出
- 自定义字段选择

## 🔧 配置说明

### 后端配置 (application.properties)

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/image_tag_system
spring.datasource.username=root
spring.datasource.password=your_password

# Redis 配置
spring.redis.host=localhost
spring.redis.port=6379

# JWT 配置
jwt.secret=your_secret_key
jwt.expiration=86400000

# 文件上传配置
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

### 前端配置

编辑 `client/src/utils/request.js` 设置后端 API 地址：

```javascript
const baseURL = 'http://localhost:8080/api'
```

## 📤 推送到 GitHub

### 方法一：使用推送脚本（推荐）

```bash
# 配置 GitHub Token（首次推送需要）
git remote set-url origin https://YOUR_TOKEN@github.com/xxh123001/AI-Precise.git

# 执行推送脚本
./push_to_github.sh
```

### 方法二：使用 SSH

```bash
# 配置 SSH
git remote set-url origin git@github.com:xxh123001/AI-Precise.git

# 推送
git push -u origin Version-1.0.1-release
```

### 方法三：手动推送

```bash
git push -u origin Version-1.0.1-release
```

## 📝 版本历史

### Version 1.0.1 (2025-10-26)
- ✅ 完整的图像标注系统
- ✅ 多用户协作功能
- ✅ 标注一致性分析
- ✅ 数据导出功能
- ✅ 任务管理系统
- ✅ 完善的 .gitignore 配置

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 License

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 👥 团队

- 开发团队：AI-Precise Team
- 项目维护：xxh123001

## 📞 联系方式

- GitHub: [xxh123001](https://github.com/xxh123001)
- 项目地址: [https://github.com/xxh123001/AI-Precise](https://github.com/xxh123001/AI-Precise)

## 🙏 致谢

感谢所有参与本项目开发和测试的团队成员！

---

⭐ 如果这个项目对您有帮助，请给我们一个 Star！



