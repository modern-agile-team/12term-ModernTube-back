# modernTubo_Backend

동영상 업로드/스트리밍 서비스(modernTube)의 Spring Boot 백엔드입니다.

## 기술 스택

- **Spring Boot 4.1.0** / Spring Framework 7.0.8 / Java 17
- **Spring Security 7.1.0** — JWT 기반 stateless 인증
- **Spring Data JPA (Hibernate 7.4.1)** + MySQL — 메인 데이터 저장소
- **MyBatis** — 일부 조회 로직에 병행 사용
- **jjwt 0.12.6** — JWT 발급/검증
- **springdoc-openapi 2.8.5** — Swagger UI / OpenAPI 3 문서
- **jasypt-spring-boot-starter 4.0.4** — `application.yaml` 민감 정보 암호화
- **net.bramp.ffmpeg 0.9.2** (+ 시스템 ffmpeg/ffprobe) — 업로드된 영상의 메타데이터(길이/해상도/코덱/비트레이트) 추출
- **spring-boot-starter-mail** — Gmail SMTP를 통한 이메일 인증번호 발송
- **Spring Data Redis** — 이메일 인증번호/로그인 시도 제한/조회수 중복 방지 등 TTL 기반 데이터 저장

## 실행 전 준비

1. **MySQL** — `spring.datasource.url`에 지정된 호스트(현재 `rockylinux-9:3306`)에 `modernTubo_db` 데이터베이스와 계정이 존재해야 함
2. **ffmpeg/ffprobe** — 로컬에 설치되어 있어야 함 (`brew install ffmpeg`). 실행 경로는 하드코딩이 아니라 `application-{profile}.yaml`의 `app.ffmpeg.ffprobe-path`/`app.ffmpeg.ffmpeg-path`로 프로필별 관리 (dev는 Homebrew 경로, prod는 Linux 경로)
3. **jasypt 마스터 비밀번호** — `application.yaml`의 `datasource.url`, `jwt.secret` 등이 `ENC(...)`로 암호화되어 있음. 실행 시 아래처럼 마스터 비밀번호를 반드시 넘겨줘야 함
   ```bash
   JASYPT_ENCRYPTOR_PASSWORD=실제비밀번호 ./gradlew bootRun
   ```
   (또는 `-Djasypt.encryptor.password=` VM 옵션)
4. **Redis** — 이메일 인증번호/로그인 시도 제한 캐시 저장소로 사용. `spring.data.redis.host/port/password`에 접속 정보 설정 필요
5. **Gmail SMTP 앱 비밀번호** — 이메일 인증번호 발송용. `spring.mail.username/password`에 설정 (일반 계정 비밀번호가 아닌 앱 비밀번호 사용)

## 인증 구조

- 로그인 성공 시 **accessToken(JWT)** + **refreshToken**을 발급
- accessToken은 응답 바디로 내려가며, 이후 요청의 `Authorization: Bearer <token>` 헤더로 전달
- refreshToken은 응답 바디에 노출되지 않고 **httpOnly Set-Cookie**(`refreshToken`, `path=/api/auth`)로만 전달됨 — 자바스크립트가 값을 읽을 수 없어 XSS로부터 보호됨. `POST /api/auth/refresh` 호출 시 서버가 이 쿠키를 자동으로 읽어 처리하므로 프론트는 별도 바디 없이 요청만 보내면 됨 (cross-origin이면 `credentials: 'include'` 필요)
- 쿠키의 `secure` 속성은 dev/create 프로필은 `false`(http), prod 프로필은 `true`(https)로 분리 설정됨 (`app.cookie.secure`)
- `deviceId`/`deviceType`/`notificationToken`으로 구성된 `DeviceInfo`를 로그인 요청에 같이 보내야 함 — 기기별로 refresh token을 독립적으로 관리하기 위함 (다중 기기 로그인 지원, 한 기기 로그아웃이 다른 기기에 영향 없음)
- 로그아웃 시 해당 accessToken을 `LoggedOutJwtTokenCache`(Redis 기반)에 등록해 만료 전에도 재사용을 막고, refreshToken 쿠키도 함께 만료시켜 삭제함
- 회원가입 직후에는 `isEmailVerified = false` 상태이며, 이메일 인증번호(`/api/auth/send-code`, `/api/auth/verify-code`)로 인증을 완료해야 로그인 가능 (`CustomUserDetails.isEnabled()`가 `emailVerified`를 체크해서 Spring Security가 자동으로 로그인을 막음)
- 로그인 실패가 5회 누적되면 5분간 해당 계정 로그인이 차단됨 (`LoginAttemptCache`, Redis 기반)

## API 엔드포인트

| Method | Path | 인증 필요 | 설명 |
|---|---|---|---|
| GET | `/api/auth/check/email` | X | 이메일 중복 확인 |
| GET | `/api/auth/check/username` | X | 아이디 중복 확인 |
| POST | `/api/auth/login` | X | 로그인 (accessToken/refreshToken 발급) |
| POST | `/api/auth/refresh` | X | refreshToken(httpOnly 쿠키)으로 accessToken 재발급, 별도 바디 불필요 |
| POST | `/api/auth/register` | X | 회원가입 |
| POST | `/api/auth/send-code` | X | 이메일 인증번호 발송 (60초 재전송 쿨다운) |
| POST | `/api/auth/verify-code` | X | 이메일 인증번호 검증 (성공 시 `isEmailVerified = true`) |
| GET | `/api/user/me` | O (USER/ADMIN) | 내 프로필 조회 |
| POST | `/api/user/logout` | O | 로그아웃 |
| GET | `/api/member/list` | O (SYSTEM) | 회원 목록 검색 (관리자) |
| GET | `/api/member/roleList` | O (SYSTEM) | 특정 유저 권한 조회 (관리자) |
| POST | `/api/member/save` | O (SYSTEM) | 회원 등록/수정 (관리자) |
| GET | `/api/member/check/username`, `/check/email` | O (SYSTEM) | 관리자용 중복 확인 |
| POST | `/api/videos/upload` | O (USER/ADMIN) | 동영상 업로드 (제목 필수, 썸네일은 선택) + 메타데이터 추출 + DB 저장 |
| GET | `/api/videos` | X | 동영상 목록 조회 (페이징, 최신순, 숨김/비활성 제외) |
| GET | `/api/videos/{id}` | X | 동영상 상세 조회 (제목/업로더/메타데이터/조회수/좋아요수/댓글수), 호출 시 조회수 증가 |
| GET | `/api/videos/{id}/stream` | X | 동영상 스트리밍 (Range 요청 기반) |
| GET | `/api/videos/{id}/thumbnail` | X | 동영상 썸네일 이미지 조회 |
| GET | `/api/videos/{id}/comments` | X | 댓글 목록 조회 (페이징) |
| POST | `/api/videos/{id}/comments` | O (USER/ADMIN) | 댓글 작성 |
| POST | `/api/videos/{id}/like` | O (USER/ADMIN) | 좋아요 토글 (누르면 등록, 다시 누르면 취소) |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 회원가입 검증 규칙 (`AuthService.registerUser`)

- 아이디: 영문 대소문자+숫자, 6자 이상
- 이메일: `local@domain.tld` 형식, TLD 2자 이상
- 비밀번호: 8자 이상, 숫자/영문 각 1개 이상 포함 (대문자 필수 아님 — 정규식상 소문자 조건과 중복됨)
- 이름: 영문자로 시작, 영문/숫자/`-`/`_` 조합 4자 이상 (한글 불가)

## Video 업로드 흐름

1. `POST /api/videos/upload` — 로그인 필요, 제목(`title`)과 함께 `video/*` 타입만 허용 (현재 최대 1GB, `application.yaml`의 `multipart.max-file-size`로 조정 가능)
2. `./streams` 폴더에 `{timestamp}_{원본파일명}`으로 저장 (디스크 저장용 파일명이며, 사용자에게 노출되는 제목과는 별개)
3. ffprobe로 duration/width/height/codec/bitrate 추출
4. 썸네일: 업로드 시 이미지(`thumbnail`)를 직접 첨부했으면 그 이미지를 그대로 저장하고, 첨부하지 않았으면 지정한 시점(`thumbnailTimestamp`, 초 단위, 미지정 시 기본값 1초)의 프레임을 ffmpeg로 추출해서 저장
5. `VIDEOS` 테이블에 업로더(FK)와 함께 저장, 저장된 정보를 응답으로 반환

## 동영상 목록 / 썸네일

- `GET /api/videos`는 숨김(`isHidden`)/비활성(`isActive`) 처리된 영상을 제외하고 최신순으로 페이징 조회 (업로더 정보는 N+1 방지를 위해 `JOIN FETCH`로 함께 조회)
- 목록 응답의 `thumbnailUrl`은 로컬 파일 경로가 아니라 `GET /api/videos/{id}/thumbnail` API 경로 형태로 내려가며, 프론트는 이 값을 그대로 `<img src>`에 사용하면 됨 (썸네일이 없는 영상은 `null`)

## 동영상 상세 조회 / 댓글 / 좋아요

- `GET /api/videos/{id}` 호출 시 제목, 업로더, 메타데이터, 조회수, 좋아요수, 댓글수를 함께 반환함
- 조회수는 같은 사용자(로그인 시 `userId`, 비로그인 시 클라이언트 IP)가 3분 이내 같은 영상을 재조회해도 중복 집계되지 않음 (`ViewCountCache`, Redis `SETNX` 기반)
- 댓글은 `Comment` 엔티티(video FK, user FK, content)로 관리하며, 상세 조회 API와 분리된 페이징 API(`GET /api/videos/{id}/comments`)로 목록을 제공
- 좋아요는 `VideoLike`(video_id + user_id 복합키)로 중복 좋아요를 방지하며, `POST /api/videos/{id}/like`로 토글(누르면 등록, 다시 누르면 취소) 처리
- 좋아요/댓글 개수는 별도 컬럼 없이 매번 COUNT 쿼리로 집계 (트래픽이 늘어나면 비정규화 고려 필요)