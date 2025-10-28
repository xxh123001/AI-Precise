# Tag Backend 重启脚本使用说明

作者：辛晓红

## 🎯 功能说明

`restart_backend.sh` 是一个自动化脚本，用于快速重启tag_backend后端服务。

## 📋 脚本功能

脚本会自动执行以下操作：

1. **停止现有服务** 🛑
   - 查找并停止所有tag_backend进程
   - 清理占用8080端口的进程
   - 支持优雅停止和强制停止

2. **清理构建文件** 🧹
   - 删除target目录
   - 清理旧的编译文件

3. **重新编译项目** 🔨
   - 执行 `mvn clean package -DskipTests`
   - 跳过测试以加快编译速度

4. **启动后端服务** 🚀
   - 后台启动Spring Boot应用
   - 自动健康检查
   - 显示启动状态

## 🚀 使用方法

### 方法一：在项目目录下执行

```bash
cd /data/hong/tag/tag_backend
./restart_backend.sh
```

### 方法二：从任意目录执行

```bash
/data/hong/tag/tag_backend/restart_backend.sh
```

## 📊 执行输出示例

```
=========================================
🚀 Tag Backend 自动重启脚本
=========================================

📍 当前目录: /data/hong/tag/tag_backend

🛑 步骤 1/4: 停止现有的tag_backend服务...
   ✅ 已停止tag_backend进程
   ✅ 8080端口已清理

🧹 步骤 2/4: 清理旧的构建文件...
   ✅ target目录已删除

🔨 步骤 3/4: 重新编译项目...
   ✅ 编译成功

🚀 步骤 4/4: 启动tag_backend服务...
   ✅ 服务已启动 (PID: 12345)
   ✅ 服务健康检查通过

=========================================
✨ Tag Backend 启动成功！
=========================================
   进程ID: 12345
   端口: 8080
   日志文件: /data/hong/tag/tag_backend/backend.log

💡 查看实时日志: tail -f backend.log
💡 停止服务: kill 12345
=========================================
```

## 🔧 常用命令

### 查看实时日志
```bash
cd /data/hong/tag/tag_backend
tail -f backend.log
```

### 查看最新100行日志
```bash
cd /data/hong/tag/tag_backend
tail -100 backend.log
```

### 手动停止服务
```bash
# 查找进程ID
ps aux | grep tag_backend

# 停止服务
kill <PID>

# 或直接使用
pkill -f tag_backend
```

### 检查服务状态
```bash
# 检查进程
ps aux | grep tag_backend

# 检查端口
lsof -i :8080

# 健康检查
curl http://localhost:8080/actuator/health
```

## ⚠️ 注意事项

1. **权限要求**
   - 脚本需要执行权限：`chmod +x restart_backend.sh`
   - 如果端口清理失败，可能需要sudo权限

2. **端口占用**
   - 脚本会自动清理8080端口
   - 如果有work_log等其他服务占用端口，会被自动停止

3. **日志文件**
   - 每次重启会清空 `backend.log`
   - 如需保留历史日志，请提前备份

4. **编译时间**
   - 首次编译需要下载依赖，时间较长
   - 后续编译通常在10-15秒

## 🐛 故障排除

### 问题1：权限不足
```bash
chmod +x restart_backend.sh
```

### 问题2：端口清理失败
```bash
# 手动清理8080端口
sudo fuser -k 8080/tcp

# 然后重新运行脚本
./restart_backend.sh
```

### 问题3：编译失败
```bash
# 检查Java版本（需要Java 17）
java -version

# 检查Maven配置
mvn -v

# 手动编译查看详细错误
mvn clean package
```

### 问题4：服务启动失败
```bash
# 查看详细日志
tail -100 backend.log

# 检查数据库连接
mysql -h localhost -u root -p123456 -e "USE image_tag_system; SELECT 1;"
```

## 📝 修改建议

如果需要修改脚本行为，可以编辑以下参数：

- **等待时间**: 修改sleep的秒数
- **健康检查URL**: 修改curl的地址
- **日志文件**: 修改backend.log的路径
- **启动配置**: 修改Java启动参数

## 🔄 自动化

可以配合cron定时任务使用（慎用）：

```bash
# 每天凌晨3点重启（示例，谨慎使用）
0 3 * * * /data/hong/tag/tag_backend/restart_backend.sh >> /var/log/tag_restart.log 2>&1
```

## 📞 技术支持

如有问题，请联系：辛晓红

---

最后更新：2025-10-16

