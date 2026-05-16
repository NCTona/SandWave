# Ball Adventure 🎮⚽

Ball Adventure là một dự án ứng dụng/game Android được xây dựng hoàn toàn bằng **Kotlin** và UI framework hiện đại **Jetpack Compose**.

## 🌟 Giới thiệu
Dự án được thiết kế chuyên biệt để chạy ở chế độ màn hình ngang (landscape), mang đến trải nghiệm đồ họa hiện đại và linh hoạt. Ball Adventure sử dụng toàn bộ hệ sinh thái của Android Jetpack để quản lý giao diện, trạng thái và luồng điều hướng của người chơi.

## 🛠️ Công nghệ & Thư viện (Tech Stack)
- **Ngôn ngữ:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Navigation:** Navigation Compose giúp quản lý việc di chuyển giữa các màn hình mượt mà.
- **System UI:** Accompanist System UI Controller (ẩn/hiện thanh trạng thái và điều hướng của hệ thống để tối ưu hóa không gian hiển thị cho game).
- **Yêu cầu hệ thống:**
  - Min SDK: 25 (Android 7.1)
  - Target SDK: 35

## 📁 Cấu trúc dự án
- Mã nguồn chính được đặt tại thư mục `app/src/main/java/com/tona/balladventure/`.
- Ứng dụng hoạt động trên một `MainActivity` duy nhất (Single-Activity Architecture) và mọi giao diện đều được vẽ thông qua các Composable functions.

## ⚙️ Hướng dẫn cài đặt

1. **Yêu cầu môi trường:** Cài đặt phiên bản **Android Studio** mới nhất.
2. Clone repository về máy tính của bạn.
3. Mở thư mục dự án bằng Android Studio.
4. Đợi Gradle tải xuống các dependencies (có cấu hình sẵn trong `build.gradle.kts`).
5. Kết nối với thiết bị Android thật (hoặc tạo một máy ảo Emulator).
6. Bấm **Run (Shift + F10)** để build và cài đặt ứng dụng.
