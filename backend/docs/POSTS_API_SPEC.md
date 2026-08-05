# 게시글 임베딩 & 유사 게시글 추천 규격서

## 1. 게시글 임베딩 저장

게시글을 **작성(POST /api/posts)** 하거나 **수정(PATCH /api/posts/{postId})** 하면, 그 게시글의 `title` + `content` 값을 `text-embedding-3-small` 모델로 임베딩하여 **`post_embeddings` 테이블**에 저장한다.

| 테이블 | 컬럼 | 값 |
|--------|------|-----|
| `post_embeddings` | `post_id` | 게시글 ID (FK) |
| `post_embeddings` | `embedding` | `title + "\n\n" + content`를 임베딩한 벡터 |
| `post_embeddings` | `model` | `text-embedding-3-small` |

- 작성 시 → 신규 저장, 수정 시 → 같은 postId의 벡터를 갱신(upsert).

---

## 2. 유사 게시글 추천 API

```
GET /api/posts/{postId}/recommendations
인증 필요: ❌
```

**요청**

| 파라미터 | 위치 | 타입 | 설명 |
|----------|------|------|------|
| `postId` | Path | Long | 기준 게시글 ID |

**응답 200**

```json
[
  {
    "rank": 1,
    "title": "얼큰한 김치찌개 맛있게 끓이는 법",
    "content": "잘 익은 김치와 돼지고기 목살을 볶아..."
  },
  {
    "rank": 2,
    "title": "구수한 된장찌개 황금 레시피",
    "content": "멸치와 다시마로 우린 육수에..."
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `rank` | int | 유사도 순위 (1~5) |
| `title` | String | 추천 게시글 제목 |
| `content` | String | 추천 게시글 내용 |

- 유사도 상위 **5개까지** 반환한다.
- 비교 대상 게시글이 5개 미만이면, 있는 개수만큼만 반환한다(부족한 개수를 채우지 않음).
