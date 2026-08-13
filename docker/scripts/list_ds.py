#!/usr/bin/env python3
"""列出数据源详情，诊断查询失败根因"""
import base64, json, urllib.request
import ddddocr

BASE = "http://localhost:8099/api"

def http(method, path, body=None, token=None):
    url = BASE + path
    h = {"Content-Type": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=h)
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read().decode())

cap = http("GET", "/auth/captcha")
cid, img = cap["data"]["captchaId"], cap["data"]["imageBase64"]
code = ddddocr.DdddOcr(show_ad=False).classification(base64.b64decode(img.split(",", 1)[-1]))
login = http("POST", "/auth/login", {"username": "admin", "password": "password", "captchaId": cid, "captchaCode": code})
tok = login["data"]["token"]
ds = http("GET", "/ds/page?current=1&size=10", token=tok)
for r in ds["data"]["records"]:
    print(json.dumps(r, ensure_ascii=False))