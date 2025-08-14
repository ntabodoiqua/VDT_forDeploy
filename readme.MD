# Hệ thống Quản lý Khóa học Trực tuyến (Online Course Management System)

Dự án này là một hệ thống backend mạnh mẽ và linh hoạt cho một nền tảng quản lý khóa học trực tuyến, được xây dựng bằng Java Spring Boot.

## Tổng quan hệ thống

Hệ thống được thiết kế để phục vụ ba vai trò người dùng chính:

*   **Học viên (Student):** Đăng ký, đăng nhập, tìm kiếm và tham gia khóa học, theo dõi tiến độ, đánh giá khóa học.
*   **Giảng viên (Instructor):** Tạo và quản lý khóa học, tải lên tài liệu, quản lý học viên.
*   **Quản trị viên (Admin):** Quản lý toàn bộ hệ thống, bao gồm người dùng, khóa học, và xem thống kê.

## Tính năng chính

*   **Xác thực & Phân quyền:** Sử dụng JWT (JSON Web Token) và Spring Security để phân quyền rõ ràng giữa các vai trò `STUDENT`, `INSTRUCTOR`, `ADMIN`.
*   **Quản lý Khóa học:** Giảng viên có thể thực hiện đầy đủ các thao tác CRUD (Tạo, Đọc, Cập nhật, Xóa) với khóa học, bài học và tài liệu.
*   **Ghi danh & Theo dõi tiến độ:** Học viên có thể ghi danh vào các khóa học và hệ thống sẽ theo dõi tiến độ học tập.
*   **Đánh giá & Nhận xét:** Học viên có thể để lại đánh giá (sao) và nhận xét cho các khóa học.
*   **Hệ thống Quiz:** Một module quiz hoàn chỉnh cho phép tạo bài kiểm tra, lưu lại lịch sử làm bài và chấm điểm tự động.
*   **API Thống kê:** Cung cấp các API cho Admin để xem dữ liệu tổng quan về hệ thống.

## Kiến trúc hệ thống

Hệ thống áp dụng kiến trúc phân lớp (Layered Architecture) rõ ràng:

*   `Controller`: Tiếp nhận request từ client.
*   `Service`: Chứa toàn bộ logic nghiệp vụ.
*   `Repository`: Tương tác với cơ sở dữ liệu qua Spring Data JPA.
*   `DTO` và `Mapper`: Truyền dữ liệu an toàn giữa các lớp.

## Công nghệ sử dụng

*   **Framework:** Spring Boot 3
*   **ORM:** Spring Data JPA / Hibernate
*   **Bảo mật:** Spring Security, JSON Web Token (JWT)
*   **Database:** MySQL
*   **Build Tool:** Maven

## Hướng dẫn cài đặt

### Yêu cầu

*   **Java JDK 21**
*   **Maven 3.8+**
*   **MySQL Server**

### Các bước cài đặt

1.  **Clone a repository về máy của bạn:**
    ```shell
    git clone <URL_CUA_REPOSITORY>
    cd VDT_backend
    ```

2.  **Tạo cơ sở dữ liệu trong MySQL:**
    *   Mở MySQL client và chạy lệnh sau để tạo database:
        ```sql
        CREATE DATABASE identity;
        ```

3.  **Cấu hình kết nối cơ sở dữ liệu:**
    *   Mở file `src/main/resources/application.yaml`.
    *   Chỉnh sửa các thông tin trong phần `datasource` nếu cần thiết (mặc định sử dụng user `root` và password `root`).
    ```yaml
    spring:
      datasource:
        url: "jdbc:mysql://localhost:3306/identity"
        username: root
        password: root
    ```

4.  **Chạy ứng dụng:**
    *   Sử dụng Maven để build và chạy dự án:
    ```shell
    mvn spring-boot:run
    ```
    *   Ứng dụng sẽ chạy tại địa chỉ `http://localhost:8080/lms`.

## Cấu trúc API

Hệ thống cung cấp một bộ RESTful API đầy đủ cho các hoạt động của người dùng:

*   **Public APIs (`/public/**`):** Dành cho người dùng không xác thực để xem danh sách khóa học, giảng viên.
*   **Authentication APIs (`/auth/**`):** Dành cho việc đăng ký, đăng nhập.
*   **Course APIs (`/courses/**`):** Dành cho việc quản lý, học tập, đánh giá khóa học.
*   **Admin APIs (`/admin/**`):** Dành cho các chức năng quản trị hệ thống.

## Đề xuất cải tiến trong tương lai

*   **Viết Test tự động:** Xây dựng Unit Test và Integration Test để đảm bảo chất lượng code.
*   **Tối ưu hóa hiệu năng:** Sử dụng Caching (Redis/Caffeine) và tối ưu các câu truy vấn CSDL.
*   **Xử lý bất đồng bộ:** Sử dụng Message Queue (RabbitMQ/Kafka) cho các tác vụ tốn thời gian.
