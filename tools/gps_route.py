#!/usr/bin/env python3
"""
에뮬레이터 주행 시연: 코스 경로를 따라 GPS 위치를 1초마다 넣는다 (adb emu geo fix).
내비와 길 위가 실제로 움직이는 위치를 받아 '그 자리에 닿는 시각'에 이야기를 트는지 확인할 수 있다.

사용: /usr/bin/python3 -I tools/gps_route.py hwaseong [속도 km/h, 기본 180] [adb 기기, 기본 emulator-5554]
경유점은 실제 도로를 대략 따라 찍은 좌표이고, 그 사이는 직선으로 잇는다.
"""
import math
import subprocess
import sys
import time

ROUTES = {
    # 서울시청 → 한강대교(노량진) → 시흥대로 → 석수(만안교) → 의왕 → 지지대 고개 → 노송지대 → 장안문 → 화성행궁 주차장
    "hwaseong": [
        (37.5663, 126.9779), (37.5440, 126.9720), (37.5240, 126.9640), (37.5154, 126.9573), (37.5080, 126.9480),
        (37.4870, 126.9100), (37.4560, 126.8990), (37.4320, 126.9030), (37.4201, 126.9061), (37.3980, 126.9260),
        (37.3700, 126.9500), (37.3450, 126.9720), (37.3225, 126.9845), (37.3151, 126.9875), (37.3000, 126.9950),
        (37.2890, 127.0130), (37.2809, 127.0147),
    ],
    # 서울시청 → 종로 → 종묘 공영주차장 (광화문·돈화문 지나감)
    "changdeokgung": [
        (37.5663, 126.9779), (37.5700, 126.9769), (37.5759, 126.9768), (37.5745, 126.9840), (37.5779, 126.9900),
        (37.5735, 126.9930), (37.5709, 126.9961),
    ],
    # 서울시청 → 잠실(송파나루) → 성남 → 남한산성 남문 → 중앙주차장
    "namhansanseong": [
        (37.5663, 126.9779), (37.5400, 127.0300), (37.5150, 127.0800), (37.5100, 127.1025), (37.4900, 127.1300),
        (37.4600, 127.1600), (37.4650, 127.1750), (37.4733, 127.1812), (37.4771, 127.1870),
    ],
}


def dist(a, b):
    r = 6371000
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dp, dl = p2 - p1, math.radians(b[1] - a[1])
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def main():
    course = sys.argv[1] if len(sys.argv) > 1 else "hwaseong"
    kmh = float(sys.argv[2]) if len(sys.argv) > 2 else 180
    dev = sys.argv[3] if len(sys.argv) > 3 else "emulator-5554"
    pts = ROUTES[course]
    step = kmh / 3.6  # 1초에 가는 거리(m)
    total = sum(dist(pts[i], pts[i + 1]) for i in range(len(pts) - 1))
    print(f"{course}: {total / 1000:.1f} km, {kmh:.0f} km/h → 약 {total / step / 60:.1f}분")
    for i in range(len(pts) - 1):
        a, b = pts[i], pts[i + 1]
        n = max(1, int(dist(a, b) / step))
        for k in range(n):
            lat = a[0] + (b[0] - a[0]) * k / n
            lng = a[1] + (b[1] - a[1]) * k / n
            subprocess.run(["adb", "-s", dev, "emu", "geo", "fix", f"{lng:.6f}", f"{lat:.6f}"], capture_output=True)
            time.sleep(1)
    lat, lng = pts[-1]
    subprocess.run(["adb", "-s", dev, "emu", "geo", "fix", f"{lng:.6f}", f"{lat:.6f}"], capture_output=True)
    print("도착")


if __name__ == "__main__":
    main()
