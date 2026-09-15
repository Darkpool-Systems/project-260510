# 댓글 알림(Web Push) 규격서

Web Push(VAPID) 방식으로 구현된 댓글/대댓글 알림 기능 규격서.
1부는 전체 흐름 설명, 2부는 프론트엔드 연동을 위한 API 규격이다.

---

## 1. 전체 흐름

1. 알림을 받으려면 먼저 브라우저에서 "알림 허용"을 눌러야 한다. 허용하면 브라우저가 나만 가지고 있는 고유한 식별 정보(구독 정보)를 하나 만들어준다.
2. 이 구독 정보를 서버로 보내서 저장해둔다. 이제 서버는 "이 사용자에게 알림을 보내려면 이 정보를 쓰면 된다"는 것을 알게 된다.
3. 다른 사람이 내 게시글에 댓글을 달면, 서버는 그 게시글의 작성자가 누구인지 확인한 뒤, 저장해둔 그 사람의 구독 정보로 알림을 보낸다.
4. 다른 사람이 내 댓글에 대댓글을 달면, 서버는 원래 댓글을 쓴 사람이 누구인지 확인한 뒤, 그 사람에게 알림을 보낸다.
5. 내가 내 글이나 내 댓글에 스스로 댓글을 달면 알림은 보내지 않는다. 본인에게 알릴 필요가 없기 때문이다.
6. 알림을 보내려고 했는데 그 구독 정보가 더 이상 유효하지 않다면(사용자가 알림을 껐거나 오래돼서 만료됨), 서버는 그 정보를 목록에서 지워버린다. 유효하지 않은 정보로 계속 알림을 시도하지 않기 위해서다.

**전체 순서**

① 알림 켜기 (최초 1회)

1. 브라우저가 사용자에게 알림 권한을 요청한다
2. 사용자가 허용하면 브라우저가 구독 정보를 생성한다
3. 프론트가 이 구독 정보를 서버로 전송한다 (`POST /api/notifications/subscribe`)
4. 서버가 DB에 저장한다

② 댓글이 달렸을 때

1. 사용자가 댓글을 작성해서 서버로 요청을 보낸다
2. 서버가 댓글을 DB에 저장한다
3. 서버가 알림 받을 사람(게시글 작성자 또는 원 댓글 작성자)의 구독 정보를 DB에서 조회한다
4. 서버가 그 구독 정보로 푸시 알림을 전송한다
5. 브라우저(Service Worker)가 알림을 받아 화면에 표시한다

---

## 2. 프론트엔드 연동 가이드

### 2.1 알림 켜기까지의 순서

1. Service Worker 파일을 만든다 (예: `public/sw.js`)
2. Service Worker 등록: `navigator.serviceWorker.register('/sw.js')`
3. 알림 권한 요청: `Notification.requestPermission()`
4. 구독 생성:
   ```js
   const registration = await navigator.serviceWorker.ready;
   const subscription = await registration.pushManager.subscribe({
     userVisibleOnly: true,
     applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
   });
   ```
5. 생성된 구독 정보를 서버에 등록 (아래 2.3 API 참고)

VAPID 공개키는 base64url 문자열이라 `applicationServerKey`에 넣으려면 `Uint8Array`로 변환해야 한다. 아래 변환 함수를 그대로 사용하면 된다.

```js
function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  return Uint8Array.from([...rawData].map((char) => char.charCodeAt(0)));
}
```

### 2.2 VAPID 공개키

```
BP2rMPi8aoZWiCav4nj2lDy2JKjNqCf2OewILKz-Nqgb-ySJok-OQG8Ch9u4dIJdVFvCm2NzPFan-RoTPCIAUt4
```

(개인키는 서버(`.env`)에만 있고 프론트에서는 필요 없다.)

### 2.3 API 명세

#### 구독 등록

```
POST /api/notifications/subscribe
인증 필요: ✅ (다른 API와 동일하게 로그인 쿠키/JWT 필요)
```

**요청 Body** — 브라우저 `subscription.toJSON()` 결과를 그대로 보내면 된다.

```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/xxxxxxxx",
  "keys": {
    "p256dh": "BN4...",
    "auth": "k8J..."
  }
}
```

**응답**: `201 Created`, body 없음

같은 `endpoint`로 다시 등록 요청을 보내면 기존 정보를 덮어쓴다(같은 브라우저 재구독, 다른 계정으로 로그인 후 재구독 등 대비).

#### 구독 해제

```
DELETE /api/notifications/subscribe?endpoint={endpoint}
인증 필요: ✅
```

**응답**: `204 No Content`

알림 끄기 버튼을 누르거나 로그아웃할 때 호출하면 된다. 없는 endpoint를 보내도 에러 없이 무시된다.

### 2.3.1 API 호출 시점 (정확히 언제 호출해야 하는가)

**절대 하면 안 되는 것**: 페이지 로드 직후 자동으로 `Notification.requestPermission()`을 호출하는 것. 유저 제스처(클릭) 없이 권한을 요청하면 브라우저가 자동으로 막거나 거부율이 매우 높아지고, 한번 "차단"으로 거부되면 유저가 브라우저 설정에 직접 들어가서 풀기 전까진 다시 요청 자체가 불가능해진다. 반드시 버튼 클릭 같은 유저 액션 안에서만 요청한다.

**구독 등록 (`POST /api/notifications/subscribe`) 호출 시점**

| 시점 | 호출 여부 | 설명 |
|------|-----------|------|
| 유저가 "알림 켜기" 버튼을 직접 클릭했을 때 | ✅ 호출 | 유일한 기본 트리거. `requestPermission()` → 허용 시 `subscribe()` → 서버 전송까지 이 클릭 핸들러 안에서 한 번에 처리 |
| 로그인 직후, `Notification.permission === 'granted'`이면서 `registration.pushManager.getSubscription()`이 `null`일 때 | ✅ 호출 | 이전에 이미 권한을 허용했었는데(다른 세션에서 켰거나) 이 브라우저엔 아직 구독이 없는 경우. `requestPermission()`은 다시 호출하지 않고(이미 granted 상태라 즉시 반환됨) `subscribe()`만 조용히 실행 |
| 로그인 직후, `Notification.permission`이 `'default'`(한 번도 물어본 적 없음)이거나 `'denied'`일 때 | ❌ 호출 안 함 | 자동으로 권한을 요청하지 않는다. 유저가 버튼을 누를 때까지 대기 |
| `pushsubscriptionchange` 이벤트 발생 시 (Service Worker 내부) | ✅ 호출 | 브라우저가 자체적으로 구독을 만료/교체했을 때 발생. 새 구독을 받아 즉시 서버에 재등록해야 알림이 안 끊긴다 |
| 페이지 진입/새로고침마다 | ❌ 호출 안 함 | 매번 부를 필요 없음. 위 "로그인 직후" 체크 한 번이면 충분 (같은 endpoint로 재등록해도 서버가 덮어쓰기 처리하긴 하지만, 불필요한 요청이므로 지양) |

**구독 해제 (`DELETE /api/notifications/subscribe`) 호출 시점**

| 시점 | 호출 여부 | 설명 |
|------|-----------|------|
| 유저가 "알림 끄기" 버튼/토글을 클릭했을 때 | ✅ 호출 | `pushManager`의 구독도 `subscription.unsubscribe()`로 같이 해제해서 브라우저 쪽 상태와 서버 쪽 상태를 맞춘다 |
| 로그아웃 버튼 클릭 시 | ✅ 호출 | **로그아웃 API를 부르기 전에** 먼저 호출해야 한다. 이 API는 인증이 필요하므로(`인증 필요: ✅`), 쿠키/토큰을 지운 뒤에는 호출할 수 없다. 순서: 구독 해제 → 로그아웃 API |
| 회원 탈퇴 시 | ✅ 호출 | 마찬가지로 탈퇴 API 호출 전에 먼저 해제. (탈퇴 시 유저 row가 삭제되면 FK로 구독 정보도 같이 지워지긴 하지만, 프론트에서 `pushManager.subscription.unsubscribe()`로 브라우저 쪽 구독까지 정리해줘야 브라우저에 죽은 구독이 안 남는다) |
| `pushsubscriptionchange`로 새 구독을 받았을 때, 이전 endpoint에 대해 | ❌ 별도 호출 안 함 | 새 endpoint로 `subscribe`만 다시 호출하면 됨. 이전 endpoint는 서버가 발송 실패 시 자동으로 정리한다(무효 토큰 정리 로직) |

### 2.4 Service Worker에서 알림 받아서 표시하기

`public/sw.js`

```js
self.addEventListener('push', (event) => {
  const data = event.data.json(); // { title, body, url }

  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      data: { url: data.url },
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(clients.openWindow(event.notification.data.url));
});
```

서버가 보내는 push payload는 항상 이 3개 필드로 고정되어 있다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `title` | String | 알림 제목 (예: `"철수님이 댓글을 남겼습니다"`) |
| `body` | String | 알림 본문 (댓글/대댓글 내용 그대로) |
| `url` | String | 알림 클릭 시 이동할 주소 (`{프론트주소}/posts/{postId}`) |

### 2.5 언제 알림이 가는지 정리

| 상황 | 알림 받는 사람 | 제목 예시 |
|------|----------------|-----------|
| 내 게시글에 다른 사람이 댓글을 씀 | 게시글 작성자 | "OO님이 댓글을 남겼습니다" |
| 내 댓글에 다른 사람이 대댓글을 씀 | 원 댓글 작성자 | "OO님이 답글을 남겼습니다" |
| 본인 글에 본인이 댓글을 씀 | (알림 없음) | - |
| 본인 댓글에 본인이 대댓글을 씀 | (알림 없음) | - |
| 알림을 켠 적 없는 유저 | (알림 없음, 에러도 없음) | - |

- 대댓글은 1단계까지만 가능하므로(대댓글에 또 대댓글 불가) 알림 대상도 항상 "게시글 작성자" 또는 "원 댓글 작성자" 둘 중 하나로 명확하다.
- 알림 발송은 댓글 작성 API 응답 이후 비동기로 처리된다. 즉 댓글 작성 요청의 응답 속도는 알림 발송 여부와 무관하다.

### 2.6 iOS 관련 필수 주의사항

iOS Safari는 일반 브라우저 탭 상태에서는 푸시 알림을 지원하지 않는다. 사용자가 사이트를 **홈 화면에 추가(PWA 설치)** 해야만 iOS에서 알림을 받을 수 있다 (iOS 16.4 이상).

- PC(Chrome/Edge/Firefox), Android(Chrome): 설치 없이 바로 동작
- iOS(Safari): `manifest.json` 작성 + "홈 화면에 추가" 유도 UI가 있어야 알림이 동작함

### 2.7 프론트 체크리스트

- [ ] `manifest.json` 작성 (iOS PWA 설치 지원용)
- [ ] `public/sw.js` 작성 — `push`, `notificationclick` 이벤트 핸들러
- [ ] "알림 켜기" 버튼 → 권한 요청 → 구독 생성 → `POST /api/notifications/subscribe` 플로우
- [ ] "알림 끄기"/로그아웃 시 `DELETE /api/notifications/subscribe` 호출
- [ ] 알림 클릭 시 해당 게시글로 이동하는지 테스트
- [ ] iOS에서는 "홈 화면에 추가" 안내 후 알림이 실제로 오는지 실기기 테스트

---

## 3. 참고 — 백엔드 구현 위치

| 역할 | 파일 |
|------|------|
| 구독 정보 엔티티 | `domain/notification/domain/PushSubscription.java` |
| 구독 등록/해제 API | `domain/notification/controller/PushSubscriptionController.java` |
| 실제 발송 로직 | `domain/notification/service/PushNotificationService.java` |
| 댓글/대댓글 알림 트리거 | `domain/notification/listener/CommentNotificationListener.java` |
| VAPID 설정 | `domain/notification/config/PushConfig.java` |
