#!/bin/bash

# 数据库同步管理系统前端部署脚本
# 使用方法: ./deploy.sh [环境] [版本]
# 示例: ./deploy.sh production v1.0.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查参数
ENVIRONMENT=${1:-production}
VERSION=${2:-latest}

log_info "开始部署前端应用..."
log_info "环境: $ENVIRONMENT"
log_info "版本: $VERSION"

# 检查必要的工具
check_dependencies() {
    log_info "检查依赖工具..."
    
    if ! command -v node &> /dev/null; then
        log_error "Node.js 未安装"
        exit 1
    fi
    
    if ! command -v pnpm &> /dev/null; then
        log_error "pnpm 未安装"
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        log_warning "Docker 未安装，将跳过容器化部署"
    fi
    
    log_success "依赖检查完成"
}

# 安装依赖
install_dependencies() {
    log_info "安装项目依赖..."
    pnpm install --frozen-lockfile
    log_success "依赖安装完成"
}

# 运行测试
run_tests() {
    log_info "运行测试..."
    if pnpm test:unit; then
        log_success "测试通过"
    else
        log_error "测试失败"
        exit 1
    fi
}

# 构建项目
build_project() {
    log_info "构建项目..."
    
    # 设置环境变量
    export NODE_ENV=$ENVIRONMENT
    
    # 构建
    if [ "$ENVIRONMENT" = "production" ]; then
        pnpm build
    else
        pnpm build:dev
    fi
    
    log_success "项目构建完成"
}

# 构建 Docker 镜像
build_docker_image() {
    if command -v docker &> /dev/null; then
        log_info "构建 Docker 镜像..."
        
        IMAGE_NAME="dbsync-frontend"
        IMAGE_TAG="$VERSION"
        
        docker build -t "$IMAGE_NAME:$IMAGE_TAG" .
        docker tag "$IMAGE_NAME:$IMAGE_TAG" "$IMAGE_NAME:latest"
        
        log_success "Docker 镜像构建完成: $IMAGE_NAME:$IMAGE_TAG"
    else
        log_warning "跳过 Docker 镜像构建"
    fi
}

# 部署到服务器
deploy_to_server() {
    log_info "部署到服务器..."
    
    # 这里可以添加具体的部署逻辑
    # 例如：rsync 到服务器、更新 Docker 容器等
    
    if [ "$ENVIRONMENT" = "production" ]; then
        log_info "生产环境部署..."
        # rsync -avz --delete dist/ user@server:/var/www/html/
        # ssh user@server "sudo systemctl reload nginx"
    else
        log_info "测试环境部署..."
        # 测试环境部署逻辑
    fi
    
    log_success "部署完成"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    # 检查构建产物
    if [ ! -d "dist" ]; then
        log_error "构建产物不存在"
        exit 1
    fi
    
    if [ ! -f "dist/index.html" ]; then
        log_error "index.html 不存在"
        exit 1
    fi
    
    log_success "健康检查通过"
}

# 清理
cleanup() {
    log_info "清理临时文件..."
    # 清理逻辑
    log_success "清理完成"
}

# 主函数
main() {
    log_info "=== 数据库同步管理系统前端部署 ==="
    
    check_dependencies
    install_dependencies
    
    # 如果不是生产环境，跳过测试
    if [ "$ENVIRONMENT" = "production" ]; then
        run_tests
    fi
    
    build_project
    health_check
    build_docker_image
    deploy_to_server
    cleanup
    
    log_success "=== 部署完成 ==="
    log_info "环境: $ENVIRONMENT"
    log_info "版本: $VERSION"
    log_info "访问地址: http://localhost (如果是本地部署)"
}

# 错误处理
trap 'log_error "部署失败"; exit 1' ERR

# 执行主函数
main

exit 0
