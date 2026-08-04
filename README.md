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

## 실행 전 준비

1. **MySQL** — `spring.datasource.url`에 지정된 호스트(현재 `rockylinux-9:3306`)에 `modernTubo_db` 데이터베이스와 계정이 존재해야 함
2. **ffmpeg/ffprobe** — 로컬에 설치되어 있어야 함 (`brew install ffmpeg`). `VideoService`에 경로가 하드코딩(`/opt/homebrew/bin/ffprobe`)되어 있어서, 다른 환경에 배포 시 경로 확인 필요
3. **jasypt 마스터 비밀번호** — `application.yaml`의 `datasource.url`, `jwt.secret` 등이 `ENC(...)`로 암호화되어 있음. 실행 시 아래처럼 마스터 비밀번호를 반드시 넘겨줘야 함
   ```bash
   JASYPT_ENCRYPTOR_PASSWORD=실제비밀번호 ./gradlew bootRun
   ```
   (또는 `-Djasypt.encryptor.password=` VM 옵션)

## 인증 구조

- 로그인 성공 시 **accessToken(JWT)** + **refreshToken**을 발급
- accessToken은 `Authorization: Bearer <token>` 헤더로 전달
- `deviceId`/`deviceType`/`notificationToken`으로 구성된 `DeviceInfo`를 로그인 요청에 같이 보내야 함 — 기기별로 refresh token을 독립적으로 관리하기 위함 (다중 기기 로그인 지원, 한 기기 로그아웃이 다른 기기에 영향 없음)
- 로그아웃 시 해당 토큰을 `LoggedOutJwtTokenCache`(현재 인메모리 `ExpiringMap` 기반)에 등록해 만료 전에도 재사용을 막음. **다중 서버로 확장 시 Redis로 교체 필요** (지금은 서버 1대 기준)

## API 엔드포인트

| Method | Path | 인증 필요 | 설명 |
|---|---|---|---|
| GET | `/api/auth/check/email` | X | 이메일 중복 확인 |
| GET | `/api/auth/check/username` | X | 아이디 중복 확인 |
| POST | `/api/auth/login` | X | 로그인 (accessToken/refreshToken 발급) |
| POST | `/api/auth/refresh` | X | refreshToken으로 accessToken 재발급 |
| POST | `/api/auth/register` | X | 회원가입 |
| GET | `/api/user/me` | O (USER/ADMIN) | 내 프로필 조회 |
| POST | `/api/user/logout` | O | 로그아웃 |
| GET | `/api/member/list` | O (SYSTEM) | 회원 목록 검색 (관리자) |
| GET | `/api/member/roleList` | O (SYSTEM) | 특정 유저 권한 조회 (관리자) |
| POST | `/api/member/save` | O (SYSTEM) | 회원 등록/수정 (관리자) |
| GET | `/api/member/check/username`, `/check/email` | O (SYSTEM) | 관리자용 중복 확인 |
| POST | `/api/videos/upload` | O (USER/ADMIN) | 동영상 업로드 + 메타데이터 추출 + DB 저장 |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 회원가입 검증 규칙 (`AuthService.registerUser`)

- 아이디: 영문 대소문자+숫자, 6자 이상
- 이메일: `local@domain.tld` 형식, TLD 2자 이상
- 비밀번호: 8자 이상, 숫자/영문 각 1개 이상 포함 (대문자 필수 아님 — 정규식상 소문자 조건과 중복됨)
- 이름: 영문자로 시작, 영문/숫자/`-`/`_` 조합 4자 이상 (한글 불가)

## Video 업로드 흐름

1. `POST /api/videos/upload` — 로그인 필요, `video/*` 타입만 허용 (현재 최대 1GB, `application.yaml`의 `multipart.max-file-size`로 조정 가능)
2. `./streams` 폴더에 `{timestamp}_{원본파일명}`으로 저장
3. ffprobe로 duration/width/height/codec/bitrate 추출
4. `VIDEOS` 테이블에 업로더(FK)와 함께 저장, 저장된 정보를 응답으로 반환

**알려진 제약**: 메타데이터 추출(ffprobe)이 실패하면 DB에는 기록이 안 남지만, 파일은 이미 디스크에 저장된 뒤라 `streams/`에 고아 파일이 남을 수 있음.

## 알려진 이슈 / TODO

- `net.jodah:expiringmap` 기반 로그아웃 토큰 캐시는 서버 1대 기준으로만 유효 — 다중 서버 환경으로 갈 경우 Redis로 교체 필요
- `RegistrationRequest.email` 필드가 검증 어노테이션상 null 허용(`@NullOrNotBlank`)으로 되어 있어, 실제로는 필수여야 하는지 확인 필요
- 이메일 인증(`app.token.email.verification`), 비밀번호 재설정(`app.token.password.reset`) 관련 설정만 있고 실제 기능은 아직 미구현
