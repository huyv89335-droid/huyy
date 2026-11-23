# AutoBuilder 1.0.0  ·  Minecraft 1.21.11 (Fabric, Java 21)

Mod tự động xây lại bản vẽ `.litematic` / `.schem`: đọc file trong thư mục
`schematics`, nhập toạ độ góc xây + toạ độ rương đồ, tự xoay camera mượt,
tự đi tới gần và đặt đúng loại block. GUI thuần 2D để giữ FPS trên điện thoại.

## Thông số đã kiểm chứng

| Thành phần | Phiên bản |
|---|---|
| Minecraft | 1.21.11 |
| Yarn Mappings | 1.21.11+build.6 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.141.6+1.21.11 |
| Fabric Loom | 1.19.4 |
| Gradle | 9.9.2 |
| Java | 21 |

## Build

```bash
# cần JDK 21
gradle wrapper      # nếu thư mục chưa có gradlew
gradlew build       # Windows  |  ./gradlew build trên Linux/macOS/Termux
```

File kết quả: `build/libs/autobuilder-1.0.0.jar`
(Có sẵn `.github/workflows/build.yml` để GitHub Actions build giùm bạn nếu bạn
không có máy tính: push repo lên GitHub → tab **Actions** → tải **Artifact**.)

## Cài vào game

1. Cài Fabric Loader 0.20.1 cho Minecraft 1.21.11.
2. Bỏ `fabric-api-0.141.6+1.21.11.jar` vào `.minecraft/mods`.
3. Bỏ `autobuilder-1.0.0.jar` vào `.minecraft/mods`.
4. Tạo thư mục `.minecraft/schematics` và copy bản vẽ `.litematic` / `.schem` vào đó.

## Dùng trong game

- Nhấn **K** (đổi được trong *Options → Controls → Key Binds → AutoBuilder*).
- **Chọn bản vẽ** → mod đọc file và báo số block + kích thước.
- Nhập **X Y Z** góc bắt đầu (hoặc bấm *Lấy vị trí đang đứng*).
- Nhập **X Y Z** rương đồ để mod tự đi lấy block khi hết (tuỳ chọn).
- Chọn **Xoay** 0/90/180/270° và tốc độ đặt (nghi 1→12 tick mỗi block).
- Bấm **BẬT**. Mod tự: bỏ block đã có → chọn block gần nhất → xoay camera mượt →
  đi tới gần (tự nhảy khi vướng) → chọn mặt đỡ hợp lệ → đặt block.
- Bấm **TẮT** hoặc nhấn K để mở lại menu dừng.

## Giải thích nhanh từng file

| File | Việc gì |
|---|---|
| `AutoBuilderMod` | Entrypoint chính, init config |
| `AutoBuilderClient` | Đăng ký phím K + chạy engine mỗi tick |
| `builder/Blueprint` | Bản vẽ đã đọc: toạ độ local + BlockState |
| `builder/Placement` | 1 block của bản vẽ |
| `builder/SchematicParser` | Đọc `.litematic` và `.schem` |
| `builder/BuilderEngine` | Bộ máy tự xây (di chuyển, camera, đặt block, rương) |
| `gui/BuilderScreen` | Menu chính 2D |
| `gui/SchematicListScreen` | Danh sách file bản vẽ |
| `util/NbtParser` | Trình đọc NBT thuần Java (không phụ thuộc mapping MC) |
| `util/BlockStateUtil` | Palette → BlockState, xoay state, tính yaw/pitch |
| `util/InventoryUtil` | Tìm/đổi block trong tui, chọn hotbar |
| `util/LookUtil` | Xoay camera mượt + tắt phím di chuyển |
| `util/Config` | Lưu cấu hình vào `config/autobuilder.properties` |
| `util/ChatUtil` | Thông báo chat + action bar (HUD nhẹ) |

## Cảnh báo

Đây là mod client dùng thao tác chuột/bàn phím thay bạn → **không dùng để
grief, phá server hay làm phập người chơi khác**. Chỉ dùng trong thế giới
của bạn hoặc server cho phép.
