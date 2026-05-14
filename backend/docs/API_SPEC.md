# Next.js ↔ Spring Boot 인증 연동 규격서

> **Backend**: `http://localhost:8080`
> **Frontend**: `http://localhost:3000` (Next.js)
> **인증 방식**: Google OAuth2 + JWT (HttpOnly Cookie)

---

## 1. 전체 인증 흐름

```
[Next.js]                      [Spring Boot]                [Google]
    │                               │                          │
    │  1. 구글 로그인 버튼 클릭       │                          │
    │  window.location.href =       │  2. Google으로 리다이렉트   │
    │  ".../oauth2/authorization    │────────────────────────>│
    │   /google"                    │                          │
    │                               │  3. 사용자 Google 인증     │
    │                               │<────────────────────────│
    │                               │                          │
    │  4. JWT 쿠키 세팅 + 리다이렉트  │                          │
    │<──────────────────────────────│                          │
    │  Set-Cookie: access_token=... │                          │
    │  Set-Cookie: refresh_token=...│                          │
    │                               │                          │
    │  [신규 사용자] → /change/nickname                         │
    │  [기존 사용자] → /                                        │
    │                               │                          │
    │  5. API 호출 (쿠키 자동 전송)   │                          │
    │──────────────────────────────>│                          │
    │  Cookie: access_token=...     │                          │
```

**핵심**: 프론트엔드에서 토큰을 저장/관리하는 코드가 전혀 없다. 브라우저가 HttpOnly 쿠키를 자동으로 관리한다.

---

## 2. 구글 로그인

브라우저를 백엔드 OAuth URL로 리다이렉트한다. (fetch 호출 아님!)

```
GET http://localhost:8080/oauth2/authorization/google
```

```tsx
// components/LoginButton.tsx
"use client";

export default function LoginButton() {
  const handleLogin = () => {
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
  };

  return <button onClick={handleLogin}>Google 로그인</button>;
}
```

### 로그인 성공 후 리다이렉트

| 구분 | 리다이렉트 URL | 설명 |
|------|--------------|------|
| **신규 사용자** | `http://localhost:3000/change/nickname` | 처음 가입한 사용자 |
| **기존 사용자** | `http://localhost:3000` | 이미 가입된 사용자 |

---

## 3. API 호출 설정

모든 API 호출 시 `withCredentials: true` 설정으로 쿠키를 자동 포함한다.

```tsx
// lib/api.ts
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
});

// 401 → 자동 토큰 재발급 인터셉터
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        await api.post("/api/auth/refresh");
        return api(originalRequest);
      } catch {
        window.location.href = "/login";
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 4. API 목록

### 4-1. 인증 상태 확인

```
GET /api/auth/status
인증 필요: ❌
```

| 응답 | Body |
|------|------|
| 200 | `{ "authenticated": true }` |
| 401 | `{ "authenticated": false }` |

---

### 4-2. Access Token 재발급

```
POST /api/auth/refresh
인증 필요: ❌ (refresh_token 쿠키 필요)
```

| 응답 | Body |
|------|------|
| 200 | `{ "message": "Token refreshed" }` + 새 access_token 쿠키 |
| 401 | `{ "error": "Refresh token expired" }` → 재로그인 필요 |

---

### 4-3. 로그아웃

```
POST /api/auth/logout
인증 필요: ❌
```

| 응답 | Body |
|------|------|
| 200 | `{ "message": "Logged out" }` + 쿠키 삭제 |

```tsx
const handleLogout = async () => {
  await api.post("/api/auth/logout");
  window.location.href = "/login";
};
```

---

### 4-4. 내 정보 조회

```
GET /api/user/me
인증 필요: ✅
```

**응답 200:**
```json
{
  "id": 1,
  "email": "user@gmail.com",
  "nickname": "홍길동",
  "provider": "GOOGLE",
  "role": "USER"
}
```

---

### 4-5. 닉네임 변경 ⭐

```
PATCH /api/user/nickname
Content-Type: application/json
인증 필요: ✅
```

**요청:**
```json
{ "nickname": "새닉네임" }
```

| 조건 | 결과 |
|------|------|
| 1~20자 | ✅ 성공 |
| 빈 값 | ❌ 400 |
| 20자 초과 | ❌ 400 |

**응답 200:**
```json
{
  "id": 1,
  "nickname": "새닉네임"
}
```

```tsx
const handleNicknameUpdate = async (nickname: string) => {
  const { data } = await api.patch("/api/user/nickname", { nickname });
  console.log(data.nickname); // 변경된 닉네임
};
```

> **참고**: 닉네임은 최초 Google 이름으로 설정되며, 이후 Google 프로필 변경과 무관하게 우리 서비스에서만 관리된다.

---

## 5. 토큰 정보

| 항목 | access_token 쿠키 | refresh_token 쿠키 |
|------|-------------------|-------------------|
| 용도 | API 인증 | Access Token 재발급 |
| 유효기간 | 1시간 | 7일 |
| HttpOnly | ✅ (JS 접근 불가) | ✅ (JS 접근 불가) |
| SameSite | Lax | Lax |

---

## 6. 에러 코드

| HTTP 상태 | 의미 | 대응 |
|-----------|------|------|
| 200 | 성공 | 정상 처리 |
| 400 | 잘못된 요청 (유효성 검증 실패) | 입력값 확인 |
| 401 | 인증 실패 | 토큰 재발급 시도 → 실패 시 /login |
| 403 | 권한 부족 | 권한 없음 안내 |

---

## 7. Next.js 프로젝트 구조 (권장)

```
app/
├── page.tsx                # 메인 페이지 (기존 사용자 로그인 후 도착)
├── login/
│   └── page.tsx            # 로그인 페이지 (Google 로그인 버튼)
├── change/
│   └── nickname/
│       └── page.tsx        # 닉네임 설정 페이지 (신규 사용자 전용)
├── layout.tsx
hooks/
├── useAuth.ts              # 인증 상태 확인 훅
lib/
├── api.ts                  # axios 인스턴스 (withCredentials + 인터셉터)
components/
├── LoginButton.tsx         # 구글 로그인 버튼
```

---

## 8. Google Cloud Console 설정

**승인된 리디렉션 URI:**
```
http://localhost:8080/login/oauth2/code/google
```

**승인된 JavaScript 원본:**
```
http://localhost:3000
http://localhost:8080
```

---

## 9. 연동 체크리스트

- [ ] axios 인스턴스 생성 (`withCredentials: true` + 인터셉터)
- [ ] 구글 로그인 버튼 → `window.location.href`로 백엔드 OAuth URL 호출
- [ ] `GET /api/auth/status`로 로그인 여부 확인
- [ ] `/change/nickname` 페이지 → `PATCH /api/user/nickname` 호출
- [ ] 로그아웃 → `POST /api/auth/logout` 호출
