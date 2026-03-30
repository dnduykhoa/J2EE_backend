# 🚀 Full Deployment Guide

## Spring Boot (BE) + React (FE) + SQL Server + Docker + AWS + Domain

---

# 🧱 1. Tổng quan kiến trúc

```
User → Domain → Nginx →
   ├── React Frontend
   ├── Spring Boot Backend (/api)
   └── SQL Server
```

---

# 🐳 2. Chuẩn bị Backend (Spring Boot)

## 2.1 Build file jar

```bash
mvn clean package
```

## 2.2 Dockerfile (BE)

```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

## 2.3 Build & push image

```bash
docker build -t yourdockerhub/j2ee-backend .
docker push yourdockerhub/j2ee-backend
```

---

# 🌐 3. Chuẩn bị Frontend (React)

## 3.1 Build project

```bash
npm run build
```

## 3.2 Dockerfile (FE)

```dockerfile
FROM nginx:alpine
COPY build /usr/share/nginx/html
EXPOSE 80
```

## 3.3 Build & push

```bash
docker build -t yourdockerhub/j2ee-frontend .
docker push yourdockerhub/j2ee-frontend
```

---

# 🗄️ 4. SQL Server (Docker)

```bash
docker run -e "ACCEPT_EULA=Y" \
-e "SA_PASSWORD=YourStrong@Pass1" \
-p 1433:1433 -d mcr.microsoft.com/mssql/server:2022-latest
```

## Config Spring Boot

```properties
spring.datasource.url=jdbc:sqlserver://db:1433;databaseName=yourdb
spring.datasource.username=sa
spring.datasource.password=YourStrong@Pass1
```

---

# ☁️ 5. Setup AWS EC2

## 5.1 Tạo instance

* Ubuntu
* t2.micro

## 5.2 Mở port

* 22 (SSH)
* 80 (HTTP)
* 443 (HTTPS)
* 8080 (optional)
* 1433 (SQL Server)

---

# 🔐 6. SSH vào server

```bash
ssh -i key.pem ubuntu@YOUR_EC2_IP
```

---

# 🐳 7. Cài Docker

```bash
sudo apt update
sudo apt install docker.io docker-compose -y
sudo systemctl start docker
```

---

# ⚙️ 8. Docker Compose (QUAN TRỌNG)

Tạo file `docker-compose.yml`:

```yaml
version: '3.8'

services:
  db:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      SA_PASSWORD: "YourStrong@Pass1"
      ACCEPT_EULA: "Y"
    ports:
      - "1433:1433"

  backend:
    image: yourdockerhub/j2ee-backend
    ports:
      - "8080:8080"
    depends_on:
      - db

  frontend:
    image: yourdockerhub/j2ee-frontend
    ports:
      - "80:80"
```

---

# 🚀 9. Chạy hệ thống

```bash
docker compose up -d
```

---

# 🌍 10. Domain

## DNS config

| Type | Name | Value  |
| ---- | ---- | ------ |
| A    | @    | EC2-IP |
| A    | www  | EC2-IP |

---

# 🔁 11. Nginx Reverse Proxy (BEST PRACTICE)

Cài nginx:

```bash
sudo apt install nginx -y
```

Config:

```bash
sudo nano /etc/nginx/sites-available/default
```

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:80;
    }

    location /api {
        proxy_pass http://localhost:8080;
    }
}
```

Restart:

```bash
sudo systemctl restart nginx
```

---

# 🔒 12. HTTPS (SSL)

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx
```

---

# ⚠️ 13. Lỗi thường gặp

* Không vào được web → chưa mở port
* API lỗi → sai URL
* DB không connect → sai host hoặc password

---

# 🎯 14. Kết luận

Bạn đã deploy thành công:

✔ Frontend React
✔ Backend Spring Boot
✔ SQL Server
✔ Docker + AWS
✔ Domain + HTTPS

---

# 💡 Tips nâng cao

* Dùng .env để quản lý biến môi trường
* Không hardcode password
* Dùng Docker Compose cho production
* Backup database định kỳ

---

🔥 DONE 🎉

---------------------------------------------------------------

# 📋 15. Thống kê sau khi mình đã đọc file và check BE

## ✅ Phần mình làm được và đã kiểm tra trực tiếp

* Đã đọc kỹ toàn bộ file hướng dẫn deploy này.
* Đã check backend code:
  * Có entrypoint Spring Boot (`Nhom05Application`) chạy chuẩn.
  * Có cấu hình security/cors trong `SecurityConfig`.
  * `pom.xml` đã có dependency chính cho Web, JPA, Security, SQL Server, Mail, OAuth2, JWT.
* Đã chạy test BE bằng Maven trên máy hiện tại: **pass** (`passed=2, failed=0`).
* Đã đối chiếu cấu hình deploy với code thực tế và thấy cần lưu ý:
  * Backend hiện **chưa có sẵn** `Dockerfile` trong thư mục BE.
  * Backend hiện **chưa có sẵn** `docker-compose.yml` trong thư mục BE.
  * `application.properties` đang để nhiều secret thật (DB, Gmail, OAuth, JWT, VNPAY, MOMO), cần chuyển sang biến môi trường trước khi lên production.

## 🛠️ Phần bạn cần tự setup (mình không thể làm thay từ máy local)

* AWS/EC2:
  * Tạo instance, mở security group, SSH key, cấu hình firewall.
* Domain + DNS:
  * Trỏ bản ghi A/WWW về EC2 public IP tại nhà cung cấp domain của bạn.
* SSL thực tế:
  * Chạy Certbot trên server thật, xác thực domain.
* Docker Hub / Registry:
  * Tạo repo image, đăng nhập tài khoản, push image bằng credential của bạn.
* Secret production:
  * Tự tạo và quản lý secret thật (DB password, JWT secret, OAuth secret, SMTP app password, VNPAY/MOMO key) qua `.env` hoặc secret manager.
* SQL Server production:
  * Tạo DB thật, backup/restore, phân quyền user DB, chính sách backup định kỳ.

## 🎯 Kết luận nhanh

* Mình đã hoàn thành phần **đọc + kiểm tra BE code + verify test**.
* Phần còn lại chủ yếu là **hạ tầng/credential/domain** nên bạn cần tự cấu hình trên môi trường thật.

