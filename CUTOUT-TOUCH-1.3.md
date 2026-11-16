# Sửa chạm CTS khi tràn camera ngang — 1.3/code 5

RMX3370, Android 13/Oplus RMX3370_13.1.0.601(CN01), Google 17.74.18.ve.arm64, Gboard 18.5.6.993608717. Ngày 16/11/2026. Chỉ sửa module fullscreen độc lập, không thay APK Cử chỉ realme/MiCTS/Google.

## Vấn đề và nguyên nhân sâu đã xác nhận

Fullscreen 1.2 đang cài kéo nội dung vào vùng camera và bỏ cutout insets, nhưng vẫn giữ side-system-gesture insets có khoảng camera. Ở ngang rotation 1, Google nhận systemGestures.left=200px = 90px cử chỉ + 110px safe inset camera. Close được vẽ ở [54,0][198,144], tâm (126,72), hoàn toàn nằm trong dải 200px.

Trace chỉ đọc ở tiến trình Google xác nhận:

- InputDispatcher đưa chạm tới LensientActivity.
- MotionEvent tới DecorView và LensCoordinatorLayout có xy=rawxy=(126,72), flags=0, ma trận View identity.
- Bộ lọc Oplus processGestureEvent trả false; không có filter bảo mật từ chối chạm.
- LensCoordinatorLayout chặn ACTION_DOWN ở dải side-gesture inset và nuốt đến ACTION_UP, nên nút Close không nhận sự kiện.

Phân tích tĩnh bản Google đang cài xác nhận listener lấy side insets từ WindowInsetsCompat type 16 (systemGestures), rồi onInterceptTouchEvent kiểm tra vùng mép. Bản trước chỉ xóa cutout mà không đồng bộ nguồn gesture nên hai lớp thông tin không còn thống nhất.

Khung input 1080×2400 kèm ROT_270 là cơ chế xoay hợp lệ, không phải lỗi cần bù tọa độ. Giả thuyết appBounds ban đầu chưa chỉ ra đúng nơi chặn; trace đã xác định nơi chặn là phép kiểm tra gesture inset ở Lens. Thử bù 110px không chữa được và không được dùng trong bản sửa.

## Thay đổi tổng quát

`Hooks.transformState` vẫn dùng deep copy InsetsState theo cửa sổ. Trước khi bỏ DisplayCutout trên bản sao, `rebaseSideGestures` đọc safe inset camera và frame của display hiện hành. Chỉ ở ngang, với cutout được bật cho cửa sổ đủ điều kiện, điều chỉnh các nguồn LEFT/RIGHT_GESTURES và LEFT/RIGHT_MANDATORY_GESTURES nằm ở biên display: bỏ phần safe inset tương ứng khỏi độ dày nguồn. Bỏ qua nguồn rỗng, giữ bên không có camera, không giảm âm.

Không đổi nguồn của display, InputDispatcher, raw MotionEvent, listener của app, lớp Google hoặc handler MiCTS. Không hard-code 110px/90px; dùng dữ liệu thực theo hướng xoay. Không có danh sách miễn CTS để giữ tràn camera. Điều kiện scope/ngoại lệ, khóa máy, floating/split window và SDK 33 vẫn theo cơ chế cũ. Insets navigation/IME/bottom gestures không bị xóa.

Giữ touch adapter của fullscreen 1.2 (PhoneStatusBarView không giữ vùng chạm khi bị ẩn), không quay lui về SystemUI 1.1. Bản cuối không chứa CutoutTouchTrace hoặc lớp phụ thuộc Google; verify.py kiểm tra ranh giới APK và source.

## Triển khai và cấu hình

Giữ APK 1.2 gốc tại research/cutout-touch-20261116/realme-fullscreen-1.2-rollback.apk. Dữ liệu thử nghiệm, APK Google và mã trích chỉ lưu cục bộ trong research/test-results, không đưa vào source ZIP hoặc GitHub.

Trong lúc chẩn đoán phát hiện Google chưa có scope client. Đã thêm Google qua giao diện LSPosed và kiểm tra bằng ScopeTool chỉ đọc. Không sửa scope/module cử chỉ. Scope Google giúp lấy trace và dùng adapter client, nhưng sửa nguồn InsetsState ở framework áp dụng cả cho app không có scope client; đã kiểm chứng bằng app thử độc lập. MiCTS gọi CTS của Google; không sửa APK hoặc cấu hình MiCTS để qua thử nghiệm.

Cấu hình enabled/hide/cutout đều giữ true; ngoại lệ Chrome giữ false. Giữ lựa chọn giữ nguồn do người dùng cấu hình. Khóa xoay gốc ghi nhận accelerometer_rotation=0, user_rotation=1; các phép thử dùng Activity yêu cầu orientation rõ ràng và cuối cùng trả về lock 1.

## Kiểm thử đã có

| Trường hợp | Kết quả |
|---|---|
| Ngang 90°, CTS qua giữ pill | Close 3/3, đúng tâm (126,72), tràn camera vẫn bật |
| Ngang 90°, CTS qua MiCTS MainActivity | Close 3/3 |
| Ngang 270°, CTS qua giữ pill | Close 3/3 |
| Ngang 270°, CTS qua MiCTS MainActivity | Close 3/3 |
| App thử độc lập, không có scope client | Cả hai chiều: content 0,0–2400×1080; chạm x=40 và x=2360 đều tăng đúng bộ đếm L/R |
| System gesture insets sau sửa | 90px trái/phải, bottom=96px giữ nguyên; không còn 110px khoảng camera thừa |
| Gboard 90° và 270° | Nội dung rộng 2400px, IME đẩy chiều cao còn 432px; gõ Q/P được đúng `qp` |
| Phím Gboard sát biên x=20 và x=2380 | Kết quả ghi ở key-touches-1-edge.json và key-touches-3-edge.json |
| Bản cuối sau reboot, Close qua pill ở 90° và dọc, qua MiCTS ở 270° | 3/3; kết quả riêng final-reboot-*.json, không ghi đè 12 lượt trước |
| Menu More Lens options bên camera ở 270° | Bấm đúng tâm bounds (2246,72) đã mở menu History / Delete last 30 minutes / Send Feedback / Privacy & Terms; chỉ mở rồi đóng menu |
| Gboard sát biên sau reboot cuối | Tiếp tục gõ đúng qp |

Screenshot bàn phím được xem trực tiếp, không có dải đen bên camera. Accessibility XML có thể clip một số bounds theo appBounds gốc (ví dụ nút dưới khai 2290px), nên không lấy riêng XML làm bằng chứng cảm ứng: app thử độc lập và chạm phím biên kiểm tra bằng tọa độ vật lý, ảnh và kết quả xử lý.

Đây là bản sửa dùng chung theo nguồn insets, không phải lời bảo đảm mọi app/ROM/hành vi tùy biến đều đã được kiểm thử. Chưa nghiệm thu toàn bộ game, multi-window, lock/auth flow, mọi kiểu custom view hoặc mọi bản Google tương lai. Không sửa vùng màn hình bị burn-in hay phần lỗ camera vật lý. Chưa chạy stress nhiều giờ; các lượt thành công không loại trừ lỗi hiếm khác.

## Bàn giao và quay lui

APK: build/realme-fullscreen-1.3.apk, SHA-256 af5321e1334de31b908d5c3feacbdb4eb254fdbc1a116d1210afc3dca0493639.

Tắt module trong app/LSPosed rồi reboot để hoàn tác. Đã giữ APK 1.2; không xóa dữ liệu/cấu hình để downgrade. Bộ thử độc lập dev.codex.fullscreentouchprobe là công cụ kiểm thử ngoài APK module; không cần giữ cài đặt để module hoạt động.

Đã reboot hai lần trong đợt này: lần nạp framework có trace để xác định giá trị sau sửa, lần cuối với APK 1.3/code 5 không có trace. Kiểm tra cuối user RUNNING_UNLOCKED, VIS vẫn mBound=true/mService khác null; không thay tùy chọn khóa màn hình của người dùng. App thử độc lập đã gỡ sau khi lấy bằng chứng; trên máy giữ module fullscreen và cấu hình enabled/hide/cutout=true. Bộ lọc app hệ thống của LSPosed đã kiểm tra trở về trạng thái ban đầu; giữ scope Google đã bổ sung.

Trace development chỉ đọc đã xác định lỗi; mọi class và đoạn gọi trace đã xóa khỏi src/APK bàn giao. Mã Google/MiCTS chỉ dùng đối chiếu cục bộ, không chép vào module.
