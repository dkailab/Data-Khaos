#!/usr/bin/env python3
"""验证可插拔模块后端：取验证码→OCR→登录→模块配置接口"""
import base64, json, sys, urllib.request

import ddddocr

BASE = "http://localhost:8099/api"

def http(method, path, body=None, token=None):
    url = BASE + path
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
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
    code, cap = http("GET", "/auth/captcha")
    cid, img_b64 = cap["data"]["captchaId"], cap["data"]["imageBase64"]
    captcha_code = ocr_captcha(img_b64)
    print("识别验证码:", captcha_code)
    code, login = http("POST", "/auth/login", {
        "username": "admin", "password": "password",
        "captchaId": cid, "captchaCode": captcha_code,
    })
    if login.get("code") != 0:
        print("登录失败:", json.dumps(login, ensure_ascii=False)[:300]); sys.exit(1)
    token = login["data"]["token"]
    print("登录成功")

    # 1. 能力位应含 module:config（超级管理员）
    code, perm = http("GET", "/permission/user/%s" % login["data"]["user"]["id"], token=token)
    flags = perm.get("data", {}).get("capabilityFlags") or []
    print("capabilityFlags 含 module:config:", "module:config" in flags)

    # 2. 可见模块
    code, vis = http("GET", "/permission/module-config/visible", token=token)
    print("visible count:", len(vis.get("data") or []), "code:", vis.get("code"))

    # 3. 全部配置
    code, lst = http("GET", "/permission/module-config/list", token=token)
    data = lst.get("data") or []
    print("list count:", len(data), "code:", lst.get("code"))
    must = [m for m in data if m.get("mandatory") == 1]
    print("mandatory 模块:", [m["moduleKey"] for m in must])

    # 4. 尝试隐藏一个必须模块 -> 应被拒绝
    bad = [{"moduleKey": "ds_list", "visible": 0}]
    code, r1 = http("PUT", "/permission/module-config", bad, token=token)
    print("隐藏必须模块 ds_list -> code:", r1.get("code"), "msg:", r1.get("msg"))

    # 5. 隐藏一个可配置模块 -> 应成功，然后恢复
    good = [{"moduleKey": "dev_schedule", "visible": 0}]
    code, r2 = http("PUT", "/permission/module-config", good, token=token)
    print("隐藏可配置模块 dev_schedule -> code:", r2.get("code"), "msg:", r2.get("msg"))
    restore = [{"moduleKey": "dev_schedule", "visible": 1}]
    code, r3 = http("PUT", "/permission/module-config", restore, token=token)
    print("恢复 dev_schedule -> code:", r3.get("code"))

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print("异常:", e); sys.exit(1)