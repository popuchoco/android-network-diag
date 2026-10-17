# Android Network Diag

[![Android CI](https://github.com/popuchoco/android-network-diag/actions/workflows/android.yml/badge.svg)](https://github.com/popuchoco/android-network-diag/actions/workflows/android.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

給台灣使用者的 Android 網路診斷與變更驗證工具。它不只看 Wi-Fi 圖示，而是透過 DNS、獨立 HTTPS 端點、Android 網路驗證狀態與公開 IP，確認裝置是否真的能連上外部網路。長期方向不是堆疊更多工具按鈕，而是回答「什麼變了、問題在哪一層、我剛做的調整有沒有用」。

目前介面為繁體中文、僅提供深色模式，支援 Android 8.0 以上版本。

## 功能

### 外網健檢

- 同時測試 DNS、Android 使用的 Google 204 與獨立的 Cloudflare 204 端點。
- 顯示 Android `VALIDATED`／captive portal 狀態與公開 IP。
- 飛航模式開啟且沒有活動網路時停止外部探測；若 Wi-Fi 或 VPN 仍可用則照常健檢。
- 說明目前的連線來源：
  - Wi-Fi、行動網路、乙太網路或 VPN
  - VPN 底層可觀察到的 Wi-Fi／行動網路
  - 本 App 的 DNS-only VPN 或外部 VPN
  - 本機 IP、預設閘道與不需電話權限的行動網路可用性摘要

### Change Lab

- 一鍵保存變更前基準，再以相同的 DNS、HTTPS 與 Fastly 測項重測。
- 探測前短暫等待 Android 的活動網路狀態穩定，降低切換 Wi-Fi／VPN 過程中的瞬時誤判。
- 可為變更前與變更後加入本機註記，記下當時網路情境與實際操作。
- 比較連線路徑、公開 IP、DNS 上游、Android 驗證狀態、通過項目與 Fastly 平均延遲。
- 結論保留原始證據，不用不透明的健康分數。
- 可預覽並匯出版本化 JSON 或可讀文字診斷膠囊；公開 IP、本機 IP 與閘道預設遮蔽。

### 持續監控

- 使用前景服務每 60 秒檢查一次外網。
- 首次失敗後 5 秒複查，連續失敗才判定斷線。
- 飛航模式期間暫停外部探測並更新通知與 Console。
- 結果寫入 App 私有目錄中的 CSV，不會自動上傳。
- Android 13 以上若拒絕通知權限，介面會說明對前景通知的影響。

### HiNet／Fastly 路線

- 以小型 Range request 檢查 GitHub、X 圖片與 Twitch 靜態資源等 Fastly 服務。
- 只做低流量可達性與延遲檢查，不會暗中執行大型測速。
- 可開啟 HiNet 官方測速頁面進行進一步確認。

### DNS 切換

- 內建 Cloudflare、Google 與 Quad9。
- 可透過本機 DNS-only `VpnService` 切換 UDP/IPv4 DNS。
- VPN 路由只包含合成 DNS 位址 `10.77.0.2/32`，不攔截一般網路流量。
- 也可複製 DNS-over-TLS 主機名稱並開啟 Android Private DNS 設定。
- 啟動或切換後會實際解析測試網域，顯示 DNS 路徑是否可用，而不只檢查 VPN 介面是否建立。

### 診斷 Console

- 顯示健檢、Fastly、DNS VPN、監控與權限事件。
- App 私有空間最多保留最新 500 筆。
- 可清除紀錄，或透過 Android 系統文件選擇器匯出純文字 `.log`。
- 只有使用者主動選擇目的地時才會匯出。

## 平台限制

- 一般 Android App 無法安全修改 `/system/etc/hosts`；HiNet 頁面只提供診斷，不宣稱能套用桌面版 hosts 修正。
- Android 同一時間只有一個非 root VPN 插槽，因此 DNS-only 模式不能與其他 `VpnService` 同時使用。
- DNS-only VPN 目前只支援 IPv4 UDP/53，尚未支援 IPv6、DNS over TCP 與 oversized response fallback。
- VPN 的底層傳輸來自 Android 可觀察到的 Network；多網路或廠商客製系統下可能無法完整判斷。
- 行動網路摘要刻意不要求 `READ_PHONE_STATE`，因此不讀取 SIM 識別資訊，也不保證能分辨所有雙卡狀態。

## 隱私與網路連線

本專案沒有帳號、analytics、廣告 SDK、遙測後端或遠端資料庫。診斷紀錄與監控 CSV 預設只保存在 App 私有空間。

執行對應功能時會連線至：

| 用途 | 端點 |
|---|---|
| Android 外網驗證 | Google 與 Cloudflare 204 端點 |
| DNS 測試 | `hinet.net` |
| 公開 IP | `api64.ipify.org`，失敗時才改用 `ifconfig.me/ip` |
| Fastly 路線測試 | UI 中列出的 GitHub、X、Twitch 服務網域 |
| 官方測速 | 僅在使用者點擊後開啟 `speed.hinet.net` |

公開 IP 服務必然會看到請求來源的公網位址。App 不會把取得的結果送往自己的伺服器。

## 建置與測試

需求：

- JDK 17
- Android SDK Platform 35
- Gradle 8.9（CI 會自動提供）

目前 repository 未提交 Gradle Wrapper；本機請使用已安裝的 Gradle 8.9：

```powershell
gradle assembleDebug testDebugUnitTest lint
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions 會在每次 push 與 pull request 執行 APK 組裝、單元測試與 Android lint。

## 專案結構

```text
app/src/main/java/tw/xiaoxin/networktool/
├─ MainActivity.kt                 Compose UI
├─ MainViewModel.kt                UI 狀態與診斷協調
├─ net/
│  ├─ NetworkDiagnostics.kt        DNS／HTTPS／公開 IP 健檢
│  ├─ ConnectionExplainer.kt       傳輸、VPN、IP、閘道與網路狀態說明
│  └─ FastlyDiagnostics.kt         低流量 Fastly 路線檢查
├─ monitor/NetworkMonitorService.kt
├─ dns/DnsVpnService.kt
├─ log/DiagnosticLog.kt
└─ model/
```

## 文件

- [產品策略／差異化藍圖](docs/PRODUCT_STRATEGY.md)
- [架構說明](docs/ARCHITECTURE.md)
- [系統設計](docs/SYSTEM_DESIGN.md)
- [開發決策／ADR](docs/DEVELOPMENT_DECISIONS.md)
- [產品 Roadmap](docs/ROADMAP.md)
- [Phase 3 實作計畫](docs/PHASE3_PLAN.md)
- [第三方參考與授權](THIRD_PARTY_REFERENCES.md)

## 授權

本專案採用 [Apache License 2.0](LICENSE)。第三方參考專案與其授權狀態請見 [THIRD_PARTY_REFERENCES.md](THIRD_PARTY_REFERENCES.md)。
