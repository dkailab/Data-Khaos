#!/usr/bin/env python3
"""验证连接池修复：取验证码→OCR→登录→查数据源→执行查询"""
import base64, json, sys, urllib.request

import ddddocr

BASE = "http://localhost:8099/api"

def http(method, path, body=None, token=None, headers=None):
    url = BASE + path
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    if headers:
        h.update(headers)
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}

_ocr = ddddocr.DdddOcr(show_ad=False)

def ocr_captcha(b64):
    raw = base64.b64decode(b64.split(",", 1)[-1])
    return _ocr.classification(raw)

def main():
    # 1. 取验证码
    code, cap = http("GET", "/auth/captcha")
    if code != 200 or cap.get("code") != 0:
        print("取验证码失败:", cap); sys.exit(1)
    cid, img_b64 = cap["data"]["captchaId"], cap["data"]["imageBase64"]
    captcha_code = ocr_captcha(img_b64)
    print("识别验证码:", captcha_code)

    # 2. 登录
    code, login = http("POST", "/auth/login", {
        "username": "admin", "password": "password",
        "captchaId": cid, "captchaCode": captcha_code,
    })
    if code != 200 or login.get("code") != 0:
        print("登录失败:", json.dumps(login, ensure_ascii=False)[:300]); sys.exit(1)
    token = login["data"]["token"]
    print("登录成功, token长度:", len(token))

    # 3. 查数据源列表
    code, ds = http("GET", "/ds/page?current=1&size=5", token=token)
    if code != 200 or ds.get("code") != 0:
        print("查数据源失败:", json.dumps(ds, ensure_ascii=False)[:300]); sys.exit(1)
    records = ds["data"].get("records") or []
    print("数据源数量:", len(records))
    if not records:
        print("无数据源，跳过查询验证"); return
    # 优先选 host=mysql（Docker 网络别名，容器内可达）的数据源
    target = next((r for r in records if (r.get("host") or "").lower() in ("mysql", "db", "dk-mysql")), records[0])
    dsid = target["id"]
    dbname = target.get("databaseName") or target.get("database") or ""
    print("使用数据源:", dsid, "host:", target.get("host"))

    # 4. 执行查询
    q = {"datasourceId": dsid, "databaseName": dbname, "sql": "SELECT 1 AS ok"}
    code, qr = http("POST", "/query/execute", q, token=token)
    if code != 200 or qr.get("code") != 0:
        print("查询失败:", json.dumps(qr, ensure_ascii=False)[:300]); sys.exit(1)
    print("查询成功:", json.dumps(qr["data"], ensure_ascii=False)[:300])

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print("异常:", e); sys.exit(1)