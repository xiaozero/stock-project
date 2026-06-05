#!/usr/bin/env python3
"""
股票数据获取脚本
调用 a-stock-data skill 获取股票实时行情和技术指标
"""
import sys
import json
import urllib.request

def tencent_quote(codes):
    """腾讯财经实时行情"""
    prefixed = []
    for c in codes:
        if c.startswith(("6", "9")):
            prefixed.append(f"sh{c}")
        elif c.startswith("8"):
            prefixed.append(f"bj{c}")
        else:
            prefixed.append(f"sz{c}")

    url = "https://qt.gtimg.cn/q=" + ",".join(prefixed)
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "Mozilla/5.0")
    resp = urllib.request.urlopen(req, timeout=10)
    data = resp.read().decode("gbk")

    result = {}
    for line in data.strip().split(";"):
        if not line.strip() or "=" not in line or '"' not in line:
            continue
        key = line.split("=")[0].split("_")[-1]
        vals = line.split('"')[1].split("~")
        if len(vals) < 53:
            continue
        code = key[2:]
        result[code] = {
            "name": vals[1],
            "price": float(vals[3]) if vals[3] else 0,
            "change_pct": float(vals[32]) if vals[32] else 0,
            "pe_ttm": float(vals[39]) if vals[39] else 0,
            "pb": float(vals[46]) if vals[46] else 0,
            "mcap_yi": float(vals[44]) if vals[44] else 0,
        }
    return result

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python stock_analysis.py <stock_code>")
        sys.exit(1)

    stock_code = sys.argv[1]
    quotes = tencent_quote([stock_code])

    if stock_code in quotes:
        result = {
            "code": stock_code,
            "name": quotes[stock_code]["name"],
            "price": quotes[stock_code]["price"],
            "change_pct": quotes[stock_code]["change_pct"],
            "pe_ttm": quotes[stock_code]["pe_ttm"],
            "pb": quotes[stock_code]["pb"],
            "mcap_yi": quotes[stock_code]["mcap_yi"],
        }
        print(json.dumps(result, ensure_ascii=False))
    else:
        print(json.dumps({"error": f"未找到股票 {stock_code} 的数据"}))