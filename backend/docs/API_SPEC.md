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
    │  router.push(OAuth URL)       │  2. Google으로 리다이렉트   │
    │──────────────────────────────>│────────────────────────>│
    │                               │                          │
    │                               │  3. 사용자 Google 인증     │
    │                               │<────────────────────────│
    │                               │                          │
    │  4. HttpOnly 쿠키 세팅 + 리다이렉트                        │
    │<──────────────────────────────│                          │
    │  Set-Cookie: access_token=... │  (JS 접근 불가)            │
    │  Set-Cookie: refresh_token=...│                          │
    │  → http://localhost:3000      │                          │
    │                               │                          │
    │  5. API 호출 (쿠키 자동 전송)   │                          │
    │──────────────────────────────>│                          │
    │  Cookie: access_token=...     │  (프론트 토큰 관리 불필요!) │
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

로그인 성공 시 백엔드가 쿠키를 세팅하고 `http://localhost:3000`으로 리다이렉트한다.
별도의 콜백 페이지가 필요 없다.

---

## 3. API 호출

모든 API 호출 시 `credentials: "include"`를 설정하면 브라우저가 쿠키를 자동으로 포함한다.

### fetch 사용

```tsx
const res = await fetch("http://localhost:8080/api/user/me", {
  credentials: "include",  // 쿠키 자동 포함
});
const data = await res.json();
```

### axios 사용

```tsx
// lib/api.ts
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,  // 쿠키 자동 포함
});

export default api;
```

```tsx
// 사용 예시
import api from "@/lib/api";

const { data } = await api.get("/api/user/me");
```

---

## 4. 인증 상태 확인

HttpOnly 쿠키는 JavaScript에서 읽을 수 없으므로, 로그인 여부는 API로 확인한다.

```
GET /api/auth/status
credentials: include
인증 필요: ❌
```

**응답 200 (로그인 됨):**
```json
{ "authenticated": true }
```

**응답 401 (로그인 안됨):**
```json
{ "authenticated": false }
```

### Next.js 활용 예시

```tsx
// hooks/useAuth.ts
"use client";
import { useEffect, useState } from "react";
import api from "@/lib/api";

export function useAuth() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get("/api/auth/status")
      .then(() => setIsLoggedIn(true))
      .catch(() => setIsLoggedIn(false))
      .finally(() => setLoading(false));
  }, []);

  return { isLoggedIn, loading };
}
```

```tsx
// app/page.tsx
"use client";
import { useAuth } from "@/hooks/useAuth";

export default function Home() {
  const { isLoggedIn, loading } = useAuth();

  if (loading) return <div>로딩 중...</div>;
  if (!isLoggedIn) return <LoginButton />;

  return <div>로그인 완료!</div>;
}
```

---

## 5. API 목록

### 5-1. 인증 상태 확인

```
GET /api/auth/status
인증 필요: ❌
쿠키 필요: ✅ (credentials: include)
```

| 응답 | 설명 |
|------|------|
| 200 | `{ "authenticated": true }` |
| 401 | `{ "authenticated": false }` |

---

### 5-2. 내 정보 조회

```
GET /api/user/me
인증 필요: ✅
쿠키 필요: ✅
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

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 사용자 고유 ID |
| email | String | Google 이메일 |
| nickname | String | Google 프로필 이름 |
| provider | String | GOOGLE |
| role | String | USER / ADMIN |

---

### 5-3. Access Token 재발급

```
POST /api/auth/refresh
인증 필요: ❌
쿠키 필요: ✅ (refresh_token 쿠키)
```

**응답 200:** 새 access_token 쿠키가 자동으로 세팅됨
```json
{ "message": "Token refreshed" }
```

**응답 401:** Refresh Token 만료 → 재로그인 필요
```json
{ "error": "Refresh token expired" }
```

### 자동 재발급 인터셉터

```tsx
// lib/api.ts
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // refresh_token 쿠키가 자동으로 전송됨
        await api.post("/api/auth/refresh");
        // 새 access_token 쿠키가 자동으로 세팅됨
        return api(originalRequest);
      } catch {
        window.location.href = "/login";
        return Promise.reject(error);
      }
    }
    return Promise.reject(error);
  }
);
```

---

### 5-4. 로그아웃

```
POST /api/auth/logout
인증 필요: ❌
쿠키 필요: ✅
```

**응답 200:** 쿠키 삭제됨
```json
{ "message": "Logged out" }
```

```tsx
const handleLogout = async () => {
  await api.post("/api/auth/logout");
  window.location.href = "/login";
};
```

---

## 6. 토큰 정보

| 항목 | access_token 쿠키 | refresh_token 쿠키 |
|------|-------------------|-------------------|
| 용도 | API 인증 | Access Token 재발급 |
| 유효기간 | 1시간 | 7일 |
| HttpOnly | ✅ (JS 접근 불가) | ✅ (JS 접근 불가) |
| SameSite | Lax | Lax |

---

## 7. 에러 코드

| HTTP 상태 | 의미 | Next.js 대응 |
|-----------|------|-------------|
| 200 | 성공 | 정상 처리 |
| 401 | 인증 실패 | /api/auth/refresh 시도 → 실패 시 /login 이동 |
| 403 | 권한 부족 | 권한 없음 안내 |

---

## 8. Next.js 프로젝트 구조 (권장)

```
app/
├── page.tsx              # 메인 페이지
├── login/
│   └── page.tsx          # 로그인 페이지 (Google 로그인 버튼)
├── layout.tsx            # 루트 레이아웃
hooks/
├── useAuth.ts            # 인증 상태 확인 훅
lib/
├── api.ts                # axios 인스턴스 (withCredentials + 인터셉터)
components/
├── LoginButton.tsx       # 구글 로그인 버튼
```

**콜백 페이지가 필요 없다!** 쿠키 방식이므로 백엔드가 로그인 성공 후 바로 메인 페이지(`/`)로 리다이렉트한다.

---

## 9. Google Cloud Console 설정

### 승인된 리디렉션 URI
```
http://localhost:8080/login/oauth2/code/google
```

### 승인된 JavaScript 원본
```
http://localhost:3000
http://localhost:8080
```

---

## 10. 연동 체크리스트

- [ ] axios 인스턴스 생성 (`withCredentials: true`)
- [ ] 구글 로그인 버튼 → `window.location.href`로 백엔드 OAuth URL 호출
- [ ] `useAuth` 훅으로 로그인 상태 확인 (`GET /api/auth/status`)
- [ ] 401 응답 시 자동 토큰 재발급 인터셉터 설정
- [ ] 로그아웃 버튼 → `POST /api/auth/logout` 호출
