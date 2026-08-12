#!/usr/bin/env bash
# ============================================================
# Data Khaos - 启动冒烟 / 集成测试脚本
#
# 前置：docker compose up -d --build 已执行完毕，网关可访问
# 用法： ./scripts/smoke-test.sh [BASE_URL]    默认 http://localhost:8080
#
# 覆盖链路：
#   登录(auth) → 网关鉴权(JWT) → 各下游服务(permission/datasource/
#   metadata/mart/query/visual/schedule/notification/approval)
# ============================================================
set -u

BASE_URL="${1:-http://localhost:8080}"
GATEWAY="${BASE_URL}/api"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-password}"

PASS=0
FAIL=0
FAILED_SERVICES=()

red()  { printf '\033[31m%s\033[0m\n' "$*"; }
green(){ printf '\033[32m%s\033[0m\n' "$*"; }
yellow(){ printf '\033[33m%s\033[0m\n' "$*"; }

info() { printf '\033[36m%s\033[0m\n' "$*"; }

# 请求辅助：$1=方法 $2=路径 $3=可选 header
req() {
  local method="$1" path="$2" hdr="${3:-}"
  if [ -n "$hdr" ]; then
    curl -s -m 10 -X "$method" -H "$hdr" "${GATEWAY}${path}"
  else
    curl -s -m 10 -X "$method" "${GATEWAY}${path}"
  fi
}

check() {
  local svc="$1" path="$2" expect="$3" resp
  resp="$(req GET "$path" "Authorization: Bearer ${TOKEN}")"
  if echo "$resp" | grep -q "$expect"; then
    green "  [PASS] ${svc}  ${path}"
    PASS=$((PASS+1))
  else
    red "  [FAIL] ${svc}  ${path}  响应: ${resp:0:120}"
    FAIL=$((FAIL+1))
    FAILED_SERVICES+=("${svc}")
  fi
}

info "== Data Khaos 冒烟测试 =="
info "网关: ${GATEWAY}"

# ---- 0. 等待网关就绪 ----
info "[0] 等待网关就绪 ..."
ready=0
for i in $(seq 1 60); do
  if curl -s -m 3 -o /dev/null "${GATEWAY}/auth/captcha"; then
    ready=1
    break
  fi
  sleep 2
done
if [ "$ready" -ne 1 ]; then
  red "网关 ${GATEWAY} 在 120s 内未就绪，请确认 docker compose ps 状态"
  exit 1
fi
green "网关已就绪"

# ---- 1. 验证码（白名单，无需 token）----
info "[1] 登录链路（auth）"
captcha=$(req GET "/auth/captcha")
if echo "$captcha" | grep -q '"code":0'; then
  green "  [PASS] auth  GET /api/auth/captcha"
  PASS=$((PASS+1))
else
  red "  [FAIL] auth  GET /api/auth/captcha  响应: ${captcha:0:120}"
  FAIL=$((FAIL+1))
fi

# ---- 2. 登录获取 token ----
login=$(curl -s -m 10 -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" \
  "${GATEWAY}/auth/login")
TOKEN=$(echo "$login" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('data', {}).get('token') or '')
except Exception:
    print('')
")
if [ -z "$TOKEN" ]; then
  red "登录失败：${login:0:200}"
  red "请确认 mysql 已初始化种子数据（admin/password）"
  exit 1
fi
green "  [PASS] auth  POST /api/auth/login  已获取 token (${#TOKEN} 字符)"
PASS=$((PASS+1))

# ---- 3. 未带 token 访问应被网关拦截 ----
info "[2] 网关鉴权拦截"
unauth=$(req GET "/auth/info")
if echo "$unauth" | grep -qiE '"(code)":(401|1040)|未登录|过期'; then
  green "  [PASS] gateway 无 token 请求被拦截 401"
  PASS=$((PASS+1))
else
  red "  [FAIL] gateway 无 token 请求未被拦截  响应: ${unauth:0:120}"
  FAIL=$((FAIL+1))
fi

# ---- 4. 带 token 探测各服务 ----
info "[3] 下游服务调用链（均经网关 + JWT 鉴权）"
check "auth"        "/auth/info"                 '"code":0'
check "permission"  "/permission/menu/all"       '"code":0'
check "approval"    "/approval/apply/page"       '"code":0'
check "datasource"  "/ds/page"                   '"code":0'
check "metadata"    "/meta/search?keyword=user"    '"code":0'
check "mart"        "/mart/model/page"           '"code":0'
check "query"       "/query/history"             '"code":0'
check "visual"      "/visual/dashboard/page"     '"code":0'
check "schedule"    "/schedule/job/page"         '"code":0'
check "notification" "/notify/template/page"     '"code":0'

# ---- 5. 汇总 ----
echo
info "== 汇总 =="
green  "通过: ${PASS}"
if [ "$FAIL" -gt 0 ]; then
  red "失败: ${FAIL} → ${FAILED_SERVICES[*]}"
  exit 1
fi
green "全部通过 ✔"
