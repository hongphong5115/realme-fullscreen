# Kết quả kiểm thử — 13/11/2026

Thiết bị realme RMX3370, Android 13 / SDK33, ROM RMX3370_13.1.0.601(CN01), APatch và LSPosed 1.9.2. Module độc lập `dev.codex.realmefullscreen` 1.0 / versionCode 1.

## Bằng chứng đã thu

| Kiểm tra | Kết quả |
|---|---|
| Build và ký | javac, D8, aapt2, zipalign, apksigner verify thành công; chữ ký APK v3 |
| Source ZIP độc lập | Giải nén vào thư mục sạch, build lại với toolchain chốt sẵn: thành công. classes.dex, manifest nhị phân và resources.arsc khớp byte với APK bàn giao; APK build lại dùng khóa mới nên chữ ký/hash APK khác |
| Ranh giới APK | 35 class của module/compiler; không đóng gói payload cử chỉ, công cụ ghi LSPosed, database hoặc khóa ký |
| Hook framework | WindowState requested visibility (1+1 method), getInsetsState (2 overload), DisplayPolicy.layoutWindowLw (1), WindowLayout.computeFrames (1) được tìm thấy trong ROM |
| Hook client/SystemUI | ViewRootImpl.getWindowInsets, WindowLayout.computeFrames, CommandQueue.onSystemBarAttributesChanged được nạp; log SystemUI hidden=true/false đổi theo cấu hình |
| Dọc sau reboot bản cuối | Nội dung `x=0 y=0 w=1080 h=2400`; Gboard mở thì nội dung cao 1503, nút cuối `[0,1359][1080,1503]` nằm trên bàn phím |
| Ngang 90° và 270° sau reboot bản cuối | Nội dung `2400×1080`; Gboard rộng đủ 2400, phần nội dung phía trên cao 432. Kết quả trong finalboot-1.json và finalboot-3.json |
| Công tắc chung | Tắt: `x=0 y=110 w=1080 h=2290`, hàng trạng thái gốc hiện lại. Bật: `x=0 y=0 w=1080 h=2400` |
| Công tắc hide riêng | hide=false về y=110; hide=true lên y=0 |
| Công tắc cutout riêng | Khi ngang: cutout=false → `x=110 y=0 w=2290 h=1080`; cutout=true → `x=0 y=0 w=2400 h=1080` |
| Ngoại lệ Chrome | Cấu hình qua giao diện thật: except.com.android.chrome=true/false; ngoại lệ khôi phục khoảng trên, bỏ ngoại lệ kéo thanh địa chỉ lên cạnh trên. Không ép Chrome hiện thanh nếu chính app đang yêu cầu ẩn |
| Launcher | Không còn hàng giờ/pin/sóng; điều hướng màn hình chính vẫn mở được |
| Facebook | Header Facebook kéo lên cạnh trên, không còn hàng status bar riêng; chỉ mở feed, không đăng bài/bình luận |
| Cài đặt | Không còn hàng trạng thái; giữ khoảng cách/header lớn do chính Cài đặt thiết kế |
| Kéo mép trên | Vuốt ngắn không thấy hàng trạng thái; kéo dài vẫn mở thông báo/Cài đặt nhanh. Đồng hồ/ngày lớn bên trong bảng Cài đặt nhanh là nội dung của bảng, không phải hàng status bar |
| Cấu hình an toàn | SharedPreferences private; lệnh content insert bị từ chối bằng SecurityException read-only, cấu hình vẫn enabled/hide/cutout=true; không thay SELinux toàn hệ thống |
| Chẩn đoán scope | ScopeTool root đọc được enabled=1 và đủ 8 scope từ database thật; không ghi vào database |
| Chạm phím mép ngang | Chạm q tại (118,603) và p tại (2280,603); ô thử nhận đúng `qp`, không chỉ kéo giãn hình bàn phím |
| Màn hình khóa | Tắt/bật màn hình: keyguard showing=true, ảnh khóa vẫn có đồng hồ và hàng biểu tượng hệ thống; log SystemUI hidden=false. Người dùng tự mở khóa trực tiếp, không nhập mật khẩu qua ADB |
| Sau mở khóa | Keyguard showing=false/inputRestricted=false, SystemUI hidden=true; đã trả máy về màn hình chính và kết thúc thao tác kiểm thử |

Ảnh, XML và dumpsys chứa dữ liệu thiết bị được giữ cục bộ trong research/test-results, không đưa vào source ZIP. JSON thu thập không tự đồng nghĩa đạt nghiệm thu; ảnh và bounds được đối chiếu riêng. Một số bounds Accessibility ngang bị clip ở 2290 theo appBounds của ROM dù frame, cây view và ảnh đã rộng 2400; không lấy riêng số clip đó để kết luận bàn phím bị lẹm.

## Các lỗi đã phát hiện và sửa

1. Scope database của System Framework phải là `system`, không phải `android`. Package callback vẫn là android; nguồn khuyến nghị scope giữ tên package Android chuẩn.
2. XSharedPreferences mới không ghi được do môi trường APatch/LSPosed trên máy. Đã chuyển sang provider chỉ đọc và private preferences, không sửa SELinux.
3. Lần layout đầu có thể giữ cached insets cũ. Đã thêm thông báo thay đổi cấu hình: yêu cầu apply insets/layout ở client và traversal/insets notification trong framework; không dùng vòng lặp ép fullscreen.
4. Android 13 WindowInsets.Builder(copy).build() đặt lại trường compat về systemBars, làm legacy adjustResize mất IME. Đã giữ mCompatInsetsTypes và mCompatIgnoreVisibility của kết quả gốc.
5. Ngăn cài hook client trùng trong system_server khi nhiều package dùng chung tiến trình.
6. Tách công tắc camera ngang khỏi hide: tắt cutout vẫn tránh camera khi status bar đang ẩn.

## Chưa được công nhận đầy đủ

- Chưa kiểm thử dài ngày hoặc thống kê lỗi hiếm; không khẳng định mọi app đều tương thích.
- Yêu cầu mở cửa sổ freeform qua am --windowingMode 6 không tạo được freeform thực trên ROM này. Chưa nghiệm thu đầy đủ cửa sổ nổi, chia đôi màn hình, mọi hộp thoại xác thực và mọi app immersive. Mã chỉ áp dụng app fullscreen mode 1, giữ các chế độ còn lại theo hệ thống; IME có xử lý riêng.
- Không tự thao tác xác thực sinh trắc/PIN hoặc đổi cài đặt khóa; người dùng đã tự mở khóa sau bài kiểm tra giao diện khóa. Chưa nghiệm thu mọi hộp thoại xác thực trong ứng dụng.
- Không chạy kiểm thử Cử chỉ realme, CTS, Gemini hoặc Wallet theo yêu cầu. Không tuyên bố đã xác minh những chức năng đó trong đợt này.
- Chưa thử thực thi rollback toàn bộ rồi bật lại module. Snapshot và câu lệnh rollback được chuẩn bị; công tắc tắt/ngoại lệ đã thử trên thiết bị.

## Cấu hình bàn giao

enabled=true, hide=true, cutout=true; Chrome không còn ngoại lệ. Adapter cũ `dev.phong.edgetoedge` bị tắt trong LSPosed nhưng APK và dữ liệu được giữ nguyên. Scope mới: System Framework (`system`), SystemUI, Launcher, Gboard, Chrome, Cài đặt, Facebook và chính module. Các module khác không bị đổi enabled/scope.

APK SHA256: `be3cb7e2a77023becda8cf3cca2ebf73195c0617b08a9584eb831bca624a9bc8`.


## Bản 1.1 — khôi phục icon trong bảng kéo xuống (2026-11-13)

- Vấn đề: bản 1.0 chặn vẽ cả icon pin/sóng/Wi-Fi trong header thông báo và Cài đặt nhanh.
- Nguyên nhân: hook ViewGroup.drawChild lọc các ID QS khi cấu hình hide đang bật, không phân biệt chúng với hàng trạng thái ngoài bảng.
- Sửa: bỏ hoàn toàn hook drawChild của QS; giữ riêng PhoneStatusBarView.dispatchDraw cho hàng trạng thái ngoài bảng. Header QS dùng vòng đời hiển thị gốc của ROM, không thêm polling hay ép visibility.
- Build/chữ ký và verify.py: đạt; cài cập nhật trên RMX3370 thành công, versionName 1.1/versionCode 2, giữ dữ liệu cấu hình.
- Đã xem ảnh chụp thật ở 4 trạng thái dọc: Home ẩn icon; bảng thông báo hiện pin/sóng/Wi-Fi; QS mở rộng hiện icon; đóng bảng trở lại Home ẩn icon. Có mở ProbeActivity để nạp lại provider sau cập nhật.
- Khởi động lại riêng SystemUI để nạp hook; không reboot máy. Hai lần restart SystemUI trong đợt này; chưa kiểm thử lại sau reboot hoặc ma trận ngang/Gboard vì hook framework/IME không đổi.
- Không kiểm thử CTS/Gemini/Wallet hay thay đổi module Cử chỉ realme. Giới hạn kiểm thử bản 1.0 còn giữ nguyên; ảnh/log chỉ lưu cục bộ.
- APK SHA256: 8dbd32bb68c957c80ec97778b6015b78df27fac445ef1dacd22f194c7ff074a0.


## 1.2 — chạm xuyên vùng status bar (2026-11-16)

Vấn đề: 1.1 chỉ ngừng vẽ icon; cửa sổ StatusBar vẫn giữ vùng chạm nên app không nhận được thao tác trong diện tích đã mở rộng.

Thay đổi: thêm OnComputeInternalInsetsListener vào root riêng của PhoneStatusBarView. Khi uiHidden=true, đặt touchableInsets=REGION và touchableRegion rỗng. Khi không áp dụng/khóa màn hình, không sửa InternalInsetsInfo. Không đổi chiều cao hay flag cửa sổ, không thu vùng chạm của notification shade, không phát lại MotionEvent. Giữ bộ nhận vuốt native SystemGesturesPointerEventListener -> DisplayPolicy.requestTransientBars -> transferTouch. Đã đối chiếu DisplayPolicy và StatusBarWindowController từ artifact ROM. Proxy giữ weak reference tới bar; chỉ cài một lần/root. Chưa kiểm chứng trên ROM khác.

Kiểm tra trên RMX3370 Android 13: build/sign/verify đạt; cài giữ dữ liệu thành công. Log Probe ghi DOWN/UP tại y=20,65 dọc; y=20 ngang 90/270, cả sau đóng bảng. Vuốt dọc và 270 gửi CANCEL cho app rồi mở notification shade, icon pin/sóng/Wi-Fi hiển thị. 90 độ một lần ngay sau xoay chưa mở; hai lần thử lại sau ổn định mở được. Không đủ mẫu để kết luận loại bỏ lỗi hiếm/ngay sau xoay. Ảnh và log lưu riêng trong test-results, không đưa vào source ZIP.

Chỉ restart riêng SystemUI (hai lần trong đợt), không reboot hay test CTS/Gemini/Wallet. Chưa kiểm tra lại lockscreen/ngoại lệ/global off trên thiết bị cho bản 1.2; nhánh trả chính sách gốc giữ nguyên. Đã khôi phục cài đặt xoay, đóng shade và về Home.

APK: realme-fullscreen-1.2.apk; SHA256 baaa75a30cfca056a82a2c94e057c2d3f2b9e48395e82275872f37d8e7ae765d. Mã nguồn và APK 1.2 nằm trong commit; chưa tạo GitHub Release 1.2.
