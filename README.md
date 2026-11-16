# Ẩn thanh trạng thái realme 1.3

[![Build APK](https://github.com/hongphong5115/realme-fullscreen/actions/workflows/build.yml/badge.svg)](https://github.com/hongphong5115/realme-fullscreen/actions/workflows/build.yml)

[Tải APK và source ZIP](https://github.com/hongphong5115/realme-fullscreen/blob/main/release/realme-fullscreen-1.3.apk) · [Kết quả kiểm thử](TESTING.md) · [Báo lỗi](https://github.com/hongphong5115/realme-fullscreen/issues)

Module LSPosed độc lập, package `dev.codex.realmefullscreen`, dành cho realme RMX3370 Android 13 / RMX3370_13.1.3.601(CN01). Không sửa hoặc phụ thuộc [Cử chỉ realme](https://github.com/hongphong5115/realme-gestures).

**Chưa nghiệm thu đầy đủ mọi app, cửa sổ nổi/chia đôi và hộp thoại xác thực.** Xem TESTING.md trước khi cài; không coi build CI thành công là đã kiểm thử điện thoại. Module không sửa burn-in đã có.

## Bản 1.3 — giữ cảm ứng khi tràn camera

Đồng bộ side-system-gesture insets với phần diện tích camera đã mở rộng. Trên ROM đã kiểm tra, dải 200px trở về 90px cử chỉ thực; bỏ phần 110px khoảng camera thừa làm Google CTS nuốt chạm nút Close. Sửa ở framework theo hình học cutout, giữ tràn camera và giữ bộ nhận cử chỉ native, không có ngoại lệ riêng CTS/MiCTS.

Đã kiểm tra 15/15 lượt đóng CTS, gồm hai chiều ngang, dọc và sau reboot; mở qua giữ pill và MiCTS. App thử ngoài scope client nhận chạm sát hai mép; Gboard phủ đủ chiều rộng và gõ được phím sát biên. Không coi số lượt này là bảo đảm mọi app hoặc lỗi hiếm dài ngày. [Cơ chế, kết quả và giới hạn 1.3](CUTOUT-TOUCH-1.3.md).

## Vấn đề và giải pháp

Ẩn biểu tượng đơn thuần không giải phóng diện tích màn hình. Ứng dụng còn nhận status-bar insets, stable insets và khoảng tránh display cutout; bàn phím là cửa sổ IME riêng, không phải Activity.

Module sử dụng ba adapter, chỉ bật trên SDK 33:

- Framework: `WindowState.getRequestedVisibility/getRequestedVisibilities` trả yêu cầu ẩn riêng status bar (internal type 0), `getInsetsState` tạo bản sao theo cửa sổ; bỏ nguồn status bar và cutout khi phù hợp. Không sửa nguồn insets toàn màn hình.
- Layout: `DisplayPolicy.layoutWindowLw` đánh dấu cửa sổ đang được bố trí; `WindowLayout.computeFrames` nhận bản sao LayoutParams/insets sau bước thích nghi Oplus. Bỏ fit status bar, đặt cutout ALWAYS; giữ nguyên navigation, IME và các nguồn cử chỉ. Hook client dùng cùng nguyên tắc cho lần layout cục bộ.
- SystemUI: bỏ vẽ các con của `PhoneStatusBarView`, giữ nguyên cửa sổ và vùng nhận chạm. Header thông báo/Cài đặt nhanh giữ nguyên icon pin, sóng, Wi-Fi, đồng hồ và nhà mạng của ROM; chỉ hàng trạng thái ngoài bảng kéo xuống bị ẩn. Không ẩn giao diện khóa màn hình.
- App/Gboard: điều chỉnh kết quả `ViewRootImpl.getWindowInsets`, gồm cả insets không phụ thuộc visibility; không thay listener hoặc padding toàn bộ cây view. Gboard nhận layout/cutout theo cửa sổ TYPE_INPUT_METHOD.

Luồng cấu hình: UI → SharedPreferences private → ContentProvider chỉ đọc → bộ nhớ đệm từng tiến trình. IPC chạy trên worker, không giữ khóa WindowManager hoặc luồng vẽ khi gọi provider. Đọc lại theo nhu cầu, tối đa một lần/giây; không tự phát cử chỉ hoặc lặp gọi fullscreen. Khi chưa có cấu hình, mặc định không can thiệp. Provider không cho insert/update/delete hoặc gọi phương thức ghi.

Đã loại bỏ cách dùng XSharedPreferences API 93: thử nghiệm trên APatch/LSPosed của máy cho thấy thư mục prefs riêng có nhãn magisk_file, ứng dụng không ghi được. Không nới SELinux để khắc phục; cấu hình mới nằm trong dữ liệu riêng của app.

## Cài đặt và cấu hình

1. Cài `realme-fullscreen-1.3.apk`. Bật module trong LSPosed.
2. Chọn **System Framework**, **SystemUI**, **Launcher hệ thống**, **Gboard**, chính module này và các app cần thích nghi client (Chrome, Facebook, Cài đặt…).
3. Tắt adapter tràn viền cũ để tránh hai bộ hook sửa cùng cửa sổ. Không xóa APK/cấu hình cũ.
4. Reboot, mở module để khởi tạo công tắc. Mặc định enabled/hide/cutout đều bật.
5. Mở lại app sau khi đổi công tắc/ngoại lệ. Thay scope hoặc hook cần reboot.

LSPosed lưu System Framework dưới tên `system` trong database, dù package được hook là `android`. Giao diện “Đọc scope (root)” đọc database thật để phân biệt app đã chọn/chưa chọn; quyền root chỉ dùng khi bấm nút. Framework có thể áp dụng một phần cho app không có scope client. Ngoại lệ khôi phục chính sách gốc của app đó, không bắt buộc hiện thanh nếu bản thân app chọn immersive.

Chế độ cutout ngang bao gồm ứng dụng bình thường và Gboard. Khi ẩn status bar ở dọc, module cũng bỏ khoảng tránh cutout trên để nội dung có thể chạm cạnh trên. Lỗ camera vật lý vẫn che nội dung tại vị trí ấy. Không thay đổi tùy chọn ẩn thanh điều hướng, trợ lý, phím nguồn hoặc FLAG_SECURE.

## Build độc lập

Windows, PowerShell 7, JDK 25; toolchain được chốt URL/SHA-256 trong `tools.lock.json`:

```powershell
git clone https://github.com/hongphong5115/realme-fullscreen.git
cd realme-fullscreen
./bootstrap.ps1
./build.ps1
python verify.py --apk build/realme-fullscreen-1.3.apk
```

Đầu ra: `build/realme-fullscreen-1.3.apk`. Có thể chỉ định `-ToolsDirectory` đến cache toolchain đã có; không cần source hoặc APK của module nào khác. Khóa ký riêng và mật khẩu ngẫu nhiên nằm trong `.local/`, không chia sẻ hoặc đóng gói. Giữ chúng để ký bản cập nhật. Build fail nếu javac/D8/aapt2/zipalign/apksigner lỗi. Tài sản giấy phép R8 nằm trong assets.

APK gốc đã kiểm tra nằm ở release/ và GitHub Release. CI tự build với khóa tạm và upload artifact `realme-fullscreen-1.3-ci`; **khác chữ ký APK gốc**, không cập nhật đè trực tiếp được. CI không giữ hoặc công bố khóa ký release.

Source ZIP có cả workflow CI. Để đóng gói source: `python package-source.py realme-fullscreen-1.3-source.zip`. Source ZIP có manifest SHA256 từng file, bỏ tools/build/.local/research/test-results và APK. Repository giữ APK gốc riêng trong release/ để đối chiếu. Công bố theo Apache-2.0; xem LICENSE, NOTICE và giấy phép R8 trong assets.

## Chẩn đoán và hoàn tác

Log tag `RealmeFullscreen`; LSPosed ghi tên hook và số method tìm được. Hook chưa tương thích phải được ghi nhận trong báo cáo kiểm thử. Giao diện có trạng thái hook tiến trình này và đọc scope thật bằng root.

Tắt công tắc, mở lại app để về chính sách gốc. Để gỡ hoàn toàn hook: tắt module trong LSPosed và reboot. Sau đó có thể bật lại adapter tràn viền cũ.

`device-tools/LsposedSetup.java` là công cụ bảo trì riêng, **không nằm trong APK**. Chế độ setup tạo snapshot SQLite nhất quán bằng VACUUM INTO trước khi đổi scope/module flags; rollback chỉ tắt module mới và khôi phục enabled của hai adapter cũ, không thay toàn bộ database đang chạy. Công cụ chỉ dùng với schema LSPosed đã kiểm tra trên máy này. Snapshot triển khai: `/data/adb/realme_fullscreen_backup_20261113/modules_config.db`.

Trên máy triển khai, có thể chạy từ ADB:

```powershell
adb shell su -c 'sh /data/local/tmp/fullscreen-rollback.sh'
adb reboot
```

Nếu không boot vào Android, vô hiệu hóa LSPosed/APatch theo cơ chế khôi phục của bản đã cài trước khi thao tác tiếp.

## Nguồn đối chiếu và giới hạn

Đối chiếu trực tiếp `services.jar`, `framework.jar`, `SystemUI.apk` của ROM; file decompile chỉ nằm trong research cục bộ, không đóng gói. Đây là adapter riêng, không tuyên bố port nguyên cơ chế Samsung.

- [Android: hide system bars](https://developer.android.com/develop/ui/views/layout/immersive)
- [Android: display cutout](https://developer.android.com/develop/ui/views/layout/display-cutout)

Không có cách tự động bảo đảm mọi app tự viết padding/header đều tràn kín. Không ép cửa sổ nổi/chia đôi màn hình thành fullscreen. Module không sửa burn-in đã có. Xem `TESTING.md` để biết trạng thái nghiệm thu thực tế; CTS/MiCTS có kiểm thử riêng trong bản 1.3; các kết quả cũ và chức năng chưa kiểm tra được ghi tách trong TESTING.md.

Kế thừa bản 1.2: status bar đang ẩn cho app nhận chạm qua vùng đó; vuốt mép trên dùng cơ chế native ROM. Một lần vuốt ngay sau xoay 90° chưa mở bảng; xem TESTING.md.
