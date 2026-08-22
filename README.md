# Split Bill Service App

Spring Boot REST API for shared expenses and settlement calculation.

## Stack

-   Java 17+
-   Spring Boot
-   Maven
-   Spring Data JPA
-   Jakarta Validation
-   Lombok
-   Flyway
-   PostgreSQL
-   Spring Security + JWT Bearer
-   Docker + Docker Compose

The challenge requires Java 17+, Spring Boot, Maven, `BigDecimal`, a
multi-stage Dockerfile, and at least one unit test covering settlement
calculation.

## Architecture

``` text
Client
 -> Spring Security / JWT
 -> Controller
 -> Service
 -> Repository / JPA
 -> PostgreSQL
```

## Database

``` text
users
  -> bill_groups
      -> bill_group_members
  -> bills
      -> bill_debtors
```

Core tables: `users`, `bill_groups`, `bill_group_members`, `bills`,
`bill_debtors`.

Money uses `BigDecimal` and PostgreSQL `NUMERIC(19,2)`.

## API

  --------------------------------------------------------------------------------------------
  Method                  Endpoint                                     Purpose
  ----------------------- -------------------------------------------- -----------------------
  POST                    `/api/v1/auth/login`                         Login and get JWT

  POST                    `/api/v1/bill-groups`                        Create group

  GET                     `/api/v1/bill-groups`                        List my groups

  GET                     `/api/v1/bill-groups/{groupId}`              Group detail

  POST                    `/api/v1/bill-groups/{groupId}/bills`        Create bill

  GET                     `/api/v1/bill-groups/{groupId}/bills`        List bills

  GET                     `/api/v1/bill-groups/{groupId}/settlement`   Calculate settlement
  --------------------------------------------------------------------------------------------

All protected endpoints use:

``` http
Authorization: Bearer <JWT>
```

JSON uses camelCase.


## HOW TO RUN

Ikuti langkah berikut dari root project ini.

### 1. Siapkan tools

Pastikan tools berikut sudah terpasang:

-   JDK 17 atau lebih baru
-   Git
-   Docker
-   Docker Compose

Cek instalasi:

``` bash
java -version
docker --version
docker compose version
```

Project ini sudah menyediakan Maven Wrapper, jadi Maven tidak perlu
di-install terpisah. Jika `mvnw` belum bisa dieksekusi di Linux/macOS,
jalankan:

``` bash
chmod +x mvnw
```

### 2. Buat file `.env`

Buat file `.env` di root project dengan isi berikut jika ingin
menyesuaikan credential, port, atau JWT secret lokal. Docker Compose
tetap punya default value, tetapi `.env` lebih jelas untuk development.

``` env
DB_NAME=split_bill
DB_PORT=5433
DB_URL=jdbc:postgresql://localhost:5433/split_bill
DB_USERNAME=split_bill
DB_PASSWORD=split_bill
SERVER_PORT=8080
JWT_SECRET=change-this-to-a-strong-32-byte-minimum-secret
```
note: JWT_SECRET harus panjang (32 byte)

Keterangan singkat:

-   `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, dan `DB_PORT` dipakai oleh
    `docker-compose.yml` untuk membuat PostgreSQL.
-   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, dan
    `JWT_SECRET` dipakai oleh aplikasi Spring Boot.
-   Saat aplikasi dijalankan melalui Docker Compose, service `backend`
    mengganti `DB_URL` menjadi `jdbc:postgresql://postgres:5432/${DB_NAME}`
    agar container backend bisa terhubung ke container PostgreSQL.
-   Ganti `JWT_SECRET` dengan secret yang kuat untuk environment selain
    lokal.
-   Contoh di atas memakai `DB_PORT=5433` agar tidak bentrok jika di
    laptop kamu sudah ada PostgreSQL lain yang berjalan di port `5432`.
    Jika port `5432` masih kosong, kamu boleh memakai `DB_PORT=5432` dan
    `DB_URL=jdbc:postgresql://localhost:5432/split_bill`.

### 3. Jalankan aplikasi dengan Docker Compose

Jalankan backend dan PostgreSQL sekaligus dalam satu Docker network
dengan Docker Compose:

``` bash
docker compose up --build -d
```

Perintah ini akan:

-   build image backend dari `Dockerfile`
-   menjalankan PostgreSQL sebagai service `postgres`
-   menjalankan aplikasi sebagai service `backend`
-   menghubungkan keduanya melalui network `split-bill-network`
-   mengarahkan koneksi backend ke
    `jdbc:postgresql://postgres:5432/split_bill`

Pastikan container `split-bill-backend` dan `split-bill-postgres` sudah
berjalan:

``` bash
docker compose ps
```

Jika ingin melihat log aplikasi:

``` bash
docker compose logs -f backend
```

Untuk memvalidasi konfigurasi Compose tanpa menjalankan container:

``` bash
docker compose config
```

Untuk memvalidasi Dockerfile saja:

``` bash
docker build .
```

Saat aplikasi start, Flyway akan menjalankan migration database dari
`src/main/resources/db/migration` secara otomatis, termasuk schema awal
dan seed data awal.

### 4. Alternatif: jalankan aplikasi dari host

Jalankan database dengan Docker Compose jika belum punya container PostgreSQL:

``` bash
docker compose up -d postgres
```

Pastikan container database sudah berjalan:

``` bash
docker compose ps
```

Jika kamu sudah punya container PostgreSQL sendiri yang berjalan di port
`5432`, kamu tidak perlu menjalankan `docker compose up -d postgres`.
Pastikan database, username, password, dan port di `.env` sesuai dengan
container tersebut, misalnya:

``` env
DB_NAME=<nama_database_di_container_kamu>
DB_PORT=5432
DB_URL=jdbc:postgresql://localhost:5432/<nama_database_di_container_kamu>
DB_USERNAME=<username_postgres_kamu>
DB_PASSWORD=<password_postgres_kamu>
```

Jalankan aplikasi Spring Boot:

``` bash
./mvnw spring-boot:run
```

Jika menggunakan Windows PowerShell, gunakan perintah berikut:

``` powershell
.\mvnw.cmd spring-boot:run
```

### 5. Akses API

Base URL aplikasi:

``` text
http://localhost:8080/api/v1
```

Sebagian endpoint membutuhkan JWT Bearer token:

``` http
Authorization: Bearer <JWT>
```

Untuk mendapatkan JWT testing API, panggil endpoint login dengan
`fullName` dan `email`:

``` bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Arif Rahman","email":"arif@example.com"}'
```

Ambil nilai `accessToken` dari response, lalu kirim ke endpoint protected
dengan header `Authorization: Bearer <accessToken>`.

Contoh cek cepat bahwa aplikasi merespons request:

``` bash
curl -i "http://localhost:8080/api/v1/bill-groups" -H "Accept: application/json"
```

Jika belum mengirim JWT, response `401 Unauthorized` berarti aplikasi
sudah hidup dan security berjalan.

### 6. Jalankan test

Untuk menjalankan test:

``` bash
./mvnw test
```

Untuk verifikasi penuh:

``` bash
./mvnw verify
```

### 7. Stop container

Setelah selesai, hentikan backend dan PostgreSQL:

``` bash
docker compose down
```

Jika ingin menghapus volume data PostgreSQL lokal juga:

``` bash
docker compose down -v
```

### 8. Alternatif: build dan jalankan JAR

Selain `spring-boot:run`, aplikasi juga bisa dijalankan dari file JAR:

``` bash
./mvnw clean package
java -jar target/backend-test-allobank-0.0.1-SNAPSHOT.jar
```

## Curl Examples

Login:

``` bash
curl -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Arif Rahman","email":"arif@example.com"}'
```

Create group:

``` bash
curl -X POST "http://localhost:8080/api/v1/bill-groups"   -H "Authorization: Bearer <JWT>"   -H "Content-Type: application/json"   -d '{"name":"Trip Bandung","memberIds":["<USER_ID_1>","<USER_ID_2>"]}'
```

List groups:

``` bash
curl "http://localhost:8080/api/v1/bill-groups"   -H "Authorization: Bearer <JWT>"   -H "Accept: application/json"
```

Group detail:

``` bash
curl "http://localhost:8080/api/v1/bill-groups/<GROUP_ID>"   -H "Authorization: Bearer <JWT>"   -H "Accept: application/json"
```

Create bill:

``` bash
curl -X POST "http://localhost:8080/api/v1/bill-groups/<GROUP_ID>/bills"   -H "Authorization: Bearer <JWT>"   -H "Content-Type: application/json"   -d '{"payerId":"<PAYER_ID>","amount":300000.00,"description":"Lunch","debtors":[{"userId":"<USER_ID_1>","amount":100000.00},{"userId":"<USER_ID_2>","amount":200000.00}]}'
```

List bills:

``` bash
curl "http://localhost:8080/api/v1/bill-groups/<GROUP_ID>/bills"   -H "Authorization: Bearer <JWT>"   -H "Accept: application/json"
```

Settlement:

``` bash
curl "http://localhost:8080/api/v1/bill-groups/<GROUP_ID>/settlement"   -H "Authorization: Bearer <JWT>"   -H "Accept: application/json"
```

## Service Charge

The challenge defines:

``` text
lowercase GitHub username: arif14377
-> sum Unicode/ASCII values
-> sum % 10
-> serviceChargePct
```

`serviceChargeAmount` is that percentage applied to total group
expenses. It must be calculated in code, not hardcoded.

## Submission Question

The hardest design decision was deciding how to model and calculate the settlement between group members.

A person's balance is not fixed because every new expense can change the overall situation. Someone who initially needs to pay another member may become a creditor after another expense is added. Because of this, I decided not to persist settlement transactions as a separate table. Instead, the settlement is derived dynamically from the existing bills and debtor allocations.

For each participant, I calculate:
```text
net balance = total paid - total owed
```

A positive balance means the participant should receive money, while a negative balance means they need to pay. I then convert these net balances into transfers while minimizing unnecessary transactions.

The trade-off I accepted was additional computation when retrieving the settlement instead of the simplicity and faster reads that could come from storing precomputed settlements. I considered this acceptable because settlement is derived data and can become stale whenever a new bill is added. Keeping it calculated from the source data avoids duplicated state and consistency problems.

I also kept payment tracking outside the core scope, so bill_debtors.amount represents the original obligation rather than a remaining balance. This keeps the model focused on the required functionality while leaving room to add payment tracking later.

## Technical Spec Documentation
https://docs.google.com/document/d/1f3w7aYGAVi9TGg07sOJwtmDjhocmBwyIOZjAB_4o9fw/edit?usp=sharing