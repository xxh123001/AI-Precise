#!/bin/bash
# 自动重启tag_backend服务脚本
# 作者：辛晓红
# 功能：停止现有后端 -> 清理 -> 重新编译 -> 启动服务

set -e  # 遇到错误立即退出

echo "========================================="
echo "🚀 Tag Backend 自动重启脚本"
echo "========================================="

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "📍 当前目录: $SCRIPT_DIR"
echo ""

# 1. 停止所有tag_backend相关的Java进程
echo "🛑 步骤 1/4: 停止现有的tag_backend服务..."

# 首先尝试优雅停止tag_backend进程
if pgrep -f "tag_backend.*jar" > /dev/null; then
    echo "   发现tag_backend进程，正在停止..."
    pkill -f "tag_backend.*jar"
    sleep 3
    
    # 检查是否还有残留进程
    if pgrep -f "tag_backend.*jar" > /dev/null; then
        echo "   强制停止残留进程..."
        pkill -9 -f "tag_backend.*jar"
        sleep 1
    fi
    echo "   ✅ 已停止tag_backend进程"
else
    echo "   ℹ️  没有运行中的tag_backend进程"
fi

# 强制清理8080端口（包括work_log等其他服务）
echo "   检查8080端口状态..."

for i in {1..3}; do
    PORT_PID=$(lsof -ti:8080 2>/dev/null || true)
    
    if [ -z "$PORT_PID" ]; then
        echo "   ✅ 8080端口空闲"
        break
    fi
    
    # 获取进程详细信息
    PORT_PROCESS=$(ps aux | grep $PORT_PID | grep -v grep | head -1 || echo "未知进程")
    echo "   ⚠️  第${i}次尝试: 端口8080被进程占用"
    echo "      进程ID: $PORT_PID"
    echo "      进程信息: $(echo $PORT_PROCESS | awk '{print $11, $12, $13}')"
    
    # 强制终止占用端口的进程
    kill -9 $PORT_PID 2>/dev/null || true
    
    # 额外清理所有可能的Java后端进程
    pkill -9 -f "work_log.*backend" 2>/dev/null || true
    pkill -9 -f "backend.*jar" 2>/dev/null || true
    fuser -k 8080/tcp 2>/dev/null || true
    
    # 等待端口释放
    sleep 3
    
    # 检查是否还有其他进程占用
    PORT_PID=$(lsof -ti:8080 2>/dev/null || true)
    if [ -z "$PORT_PID" ]; then
        echo "   ✅ 8080端口已清理"
        break
    elif [ $i -eq 3 ]; then
        echo "   ❌ 端口清理失败，使用sudo强制清理..."
        sudo fuser -k 8080/tcp 2>/dev/null || true
        sudo pkill -9 -f "java.*8080" 2>/dev/null || true
        sleep 3
        
        if lsof -ti:8080 > /dev/null 2>&1; then
            echo "   ❌ 端口仍被占用，请手动清理"
            lsof -i :8080
            exit 1
        else
            echo "   ✅ 端口强制清理成功"
        fi
    fi
done

# 2. 清理旧的构建文件
echo ""
echo "🧹 步骤 2/4: 清理旧的构建文件..."
if [ -d "target" ]; then
    rm -rf target
    echo "   ✅ target目录已删除"
fi

# 3. 重新编译项目
echo ""
echo "🔨 步骤 3/4: 重新编译项目..."
echo "   开始Maven构建..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "   ✅ 编译成功"
else
    echo "   ❌ 编译失败，请检查代码"
    exit 1
fi

# 4. 启动后端服务
echo ""
echo "🚀 步骤 4/4: 启动tag_backend服务..."

# 清空旧日志
> backend.log

# 后台启动服务
nohup java -jar -Dspring.profiles.active=prod target/tag_backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
BACKEND_PID=$!

echo "   ✅ 服务已启动 (PID: $BACKEND_PID)"
echo ""

# 等待服务启动（重试机制）
echo "⏳ 等待服务完全启动..."

# 检查服务进程
if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
    echo "   ❌ 服务进程启动失败"
    echo ""
    echo "🔍 最新日志输出:"
    tail -20 backend.log
    exit 1
fi

echo "   ✅ 服务进程正在运行 (PID: $BACKEND_PID)"

# 健康检查（最多尝试10次，每次间隔2秒）
echo "   等待Spring Boot初始化..."
HEALTH_CHECK_SUCCESS=false

for i in {1..10}; do
    sleep 2
    
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
        HEALTH_CHECK_SUCCESS=true
        break
    fi
    
    # 检查进程是否还在运行
    if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo "   ❌ 服务进程意外终止"
        tail -20 backend.log
        exit 1
    fi
    
    echo "   ⏳ 第${i}次检查...（等待中）"
done

echo ""
if [ "$HEALTH_CHECK_SUCCESS" = true ]; then
    echo "========================================="
    echo "✨ Tag Backend 启动成功！"
    echo "========================================="
    echo "   进程ID: $BACKEND_PID"
    echo "   端口: 8080"
    echo "   日志文件: $SCRIPT_DIR/backend.log"
    echo "   健康状态: UP ✅"
    echo ""
    echo "💡 查看实时日志: tail -f backend.log"
    echo "💡 停止服务: kill $BACKEND_PID 或 pkill -f tag_backend"
    echo "💡 再次重启: ./restart_backend.sh"
    echo "========================================="
else
    echo "⚠️  服务启动但健康检查未通过"
    echo "   进程仍在运行，可能需要更长时间初始化"
    echo "   查看日志: tail -f backend.log"
    echo ""
    echo "💡 手动检查: curl http://localhost:8080/actuator/health"
fi

exit 0

