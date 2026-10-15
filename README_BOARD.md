# board: 게시판, 연관관계, 댓글 REST API

`board`는 `guestbook`의 CRUD/페이징 구조에 게시글 작성자(`Member`), 댓글(`Reply`), 비동기 JSON API를 추가한 프로젝트입니다.
HTML 페이지 요청과 댓글 AJAX 요청이 한 애플리케이션 안에서 어떻게 공존하는지 공부하기 좋습니다.

## 1. 모델 관계

```text
Member(email PK)
       1
       |
       N
Board(bno PK, title, content, writer, regDate, modDate)
       1
       |
       N
Reply(rno PK, text, replyer, board, regDate, modDate)
```

### `BaseEntity`

`@MappedSuperclass`인 `BaseEntity`는 `regDate`와 `modDate`를 자식 Entity에 물려줍니다.
`Board`와 `Reply`가 상속하고, `BoardApplication`의 `@EnableJpaAuditing`과 `@CreatedDate`, `@LastModifiedDate`가 날짜를 자동 기록합니다.

### `Member`

파일: `src/main/java/com/fullstack/entity/Member.java`

`email`이 String 기본키입니다. `password`, `name`을 가지지만 회원가입/로그인 기능은 이 프로젝트에 구현되어 있지 않습니다.
게시글 등록 시 `BoardService.dtoToEntity()`는 `writerEmail`로 `Member.builder().email(...)`을 만들어 Board에 연결합니다. 즉 현재 코드는 실제 회원을 먼저 조회하고 검증하는 인증 시스템이 아니라, 작성자 이메일을 연관관계에 넣어 보는 실습입니다.

### `Board`

파일: `src/main/java/com/fullstack/entity/Board.java`

- `bno`: IDENTITY 자동 증가 기본키
- `title`, `content`
- `writer`: `@ManyToOne(fetch = FetchType.LAZY)`인 Member
- `changeTitle`, `changeContent`: 수정 메서드
- `@ToString(exclude = "writer")`: Lazy 연관 객체와 순환 참조를 피함

`LAZY`는 Board를 조회할 때 Member를 즉시 모두 가져오지 않고 실제 접근 시 조회합니다. 영속성 컨텍스트가 닫힌 뒤 `writer`에 접근하면 LazyInitializationException이 발생할 수 있으므로 DTO 변환 시점과 fetch join을 함께 이해해야 합니다.

### `Reply`

파일: `src/main/java/com/fullstack/entity/Reply.java`

- `rno`: 댓글 번호
- `text`: 댓글 내용
- `replyer`: 작성자 문자열
- `board`: `@ManyToOne(fetch = FetchType.LAZY)`

`Reply`의 `replyer`는 Member 연관관계가 아니라 문자열입니다. 또한 Board에 `cascade`/`orphanRemoval`이 없으므로 게시글 삭제 전에 댓글을 직접 지워야 합니다.

## 2. DTO와 페이지 결과

### `BoardDTO`

`Board`의 `Member writer`를 `writerEmail`, `writerName`으로 평탄화하고 `replyCount`를 추가합니다.
Entity의 연관관계를 그대로 HTML에 노출하지 않고 목록에 필요한 값만 전달하는 형태입니다.

### `ReplyDTO`

`rno`, `text`, `replyer`, `bno`, `regDate`, `modDate`를 갖습니다.
댓글 API의 JSON 요청/응답에 쓰이며, `bno`로 어느 게시글 댓글인지 표현합니다.

### `PageRequestDTO`

`page`, `size`, `type`, `keyword`를 가집니다. `getPageable(Sort)`는 화면의 1-based page를 `page - 1`로 바꿉니다.
목록 링크는 `page`, `type`, `keyword`를 함께 전달하지만 현재 Board Repository 쿼리는 type/keyword 검색을 완성하지 않은 학습 상태입니다.

### `PageResultDTO<DTO, EN>`

`Page<EN>`과 `Function<EN, DTO>`를 받아 Entity를 DTO 목록으로 변환합니다.
`start`, `end`, `prev`, `next`, `pageList`는 10개 단위 페이지 버튼을 그리기 위한 값입니다.

## 3. Repository 쿼리

### `BoardRepository`

파일: `src/main/java/com/fullstack/repository/BoardRepository.java`

#### `getBoardWithWriter(Long bno)`

```jpql
select b, w
from Board b left join b.writer w
where b.bno = :bno
```

게시글과 작성자를 `Object[]`로 함께 조회합니다. 서비스에서 `[0]`은 Board, `[1]`은 Member로 해석합니다.

#### `getBoardWithReply(Long bno)`

게시글과 댓글을 `left join`하여 상세 화면에서 필요한 조합을 가져옵니다. 댓글이 없는 게시글도 `left join` 때문에 게시글 자체는 결과에 남습니다.

#### `getBoardWithReplyCount(Pageable)`

목록용 핵심 쿼리입니다. 게시글/작성자와 `count(r)`를 함께 조회하고 `group by b, w`로 게시글별 댓글 수를 계산합니다.
`countQuery = "select count(b) from Board b"`가 있어 `Page<Object[]>`의 전체 게시글 수를 계산할 수 있습니다.

결과 배열을 반드시 다음 순서로 해석해야 합니다.

```text
row[0] -> Board
row[1] -> Member
row[2] -> Long replyCount
```

#### `getBoardByBno(Long bno)`

상세 조회용으로 게시글, 작성자, 댓글 수를 반환합니다. 집계 쿼리는 `group by`와 반환 타입이 서로 잘 맞는지 DB/Hibernate 버전에 따라 확인해야 합니다.

### `ReplyRepository`

```java
@Modifying
@Query("delete from Reply r where r.board.bno = :bno")
int deleteByBno(Long bno);

List<Reply> getRepliesByBoardOrderByRno(Board board);
```

첫 메서드는 게시글 삭제 전 댓글을 일괄 삭제합니다. 실제 애플리케이션에서는 게시글 삭제와 댓글 삭제를 `@Transactional` 하나로 묶어 중간 실패 시 일관성을 보장하는 것이 좋습니다.

## 4. Service 메서드

### `BoardService`

파일: `src/main/java/com/fullstack/service/BoardService.java`

- `dtoToEntity(BoardDTO)`: writerEmail을 Member로 만들고 Board Entity 생성
- `entityToDTO(Board, Member, Long)`: 조회 결과를 화면용 DTO로 변환
- `register(BoardDTO)`: 저장 후 `bno` 반환
- `getList(PageRequestDTO)`: 목록과 댓글 수를 페이지 DTO로 변환
- `get(Long)`: 상세 DTO 조회
- `modify(BoardDTO)`: 제목/내용을 변경 메서드로 수정
- `removeWithRepies(Long)`: 댓글을 먼저 삭제한 뒤 게시글 삭제

프로젝트 코드에는 `mdifiy`처럼 오타가 있는 메서드와 `modify`가 함께 보일 수 있으므로, 인터페이스와 구현체의 메서드명이 실제로 일치하는지 확인하면서 읽어야 합니다.

### `BoardServiceImpl.getList`

1. `Sort.by("bno").descending()` 생성
2. `PageRequestDTO.getPageable(sort)` 호출
3. `boardRepository.getBoardWithReplyCount(pageable)` 실행
4. 각 `Object[]`에서 Board/Member/댓글 수 분리
5. `entityToDTO()`로 BoardDTO 생성
6. `PageResultDTO` 반환

### `modify`

`findById(bno)`로 Board를 찾고 `changeTitle`, `changeContent`를 호출한 후 `save`합니다.
Entity의 모든 필드를 setter로 덮지 않고 수정 가능한 필드만 변경하는 것이 핵심입니다.

### 삭제

댓글에 cascade가 없으므로 `replyRepository.deleteByBno(bno)`를 먼저 호출한 다음 `boardRepository.deleteById(bno)`를 호출합니다.
삭제 순서를 반대로 하면 외래키 제약 오류가 날 수 있습니다.

## 5. HTML Controller 흐름

파일: `src/main/java/com/fullstack/controller/BoardController.java`

| 요청 | 메서드 | 역할 |
|---|---|---|
| `GET /board/` | `index()` | 목록으로 이동 |
| `GET /board/list` | `list(PageRequestDTO, Model)` | 페이지 목록 렌더링 |
| `GET /board/register` | `register()` | 등록 폼 표시 |
| `POST /board/register` | `postRegister(BoardDTO, RedirectAttributes)` | 게시글 저장 후 redirect |
| `GET /board/read?bno=...` | `read(...)` | 상세 화면 |
| `GET /board/modify?bno=...` | `read(...)` | 수정 폼 |
| `POST /board/modify` | `modify(...)` | 제목/내용 수정 |
| `POST /board/remove` | `remove(...)` | 게시글과 댓글 삭제 |

상세/수정 화면에 `requestDTO`를 함께 전달하는 이유는 목록으로 돌아갈 때 원래 페이지와 검색 파라미터를 유지하기 위해서입니다.

## 6. 댓글 REST API

파일: `src/main/java/com/fullstack/controller/ReplyController.java`

| HTTP | URL | 메서드 | 반환 |
|---|---|---|---|
| GET | `/replies/board/{bno}` | `getListByBoard` | 댓글 DTO 목록 JSON |
| POST | `/replies/` | `register` | 생성된 ReplyDTO |
| PUT | `/replies/{rno}` | `modify` | 수정된 ReplyDTO |
| DELETE | `/replies/{rno}` | `remove` | 상태 코드/응답 |

`@RestController`는 반환 객체를 View 이름이 아니라 JSON 응답 본문으로 직렬화합니다.
`@RequestBody ReplyDTO`는 AJAX가 보낸 JSON을 DTO로 변환합니다. URL의 `{rno}`, `{bno}`는 `@PathVariable`로 받습니다.

### 브라우저의 댓글 흐름

`board/read.html`은 jQuery `$.ajax`로 다음을 수행합니다.

1. 페이지가 열리면 `GET /replies/board/{bno}`로 댓글 목록을 요청합니다.
2. 등록 버튼을 누르면 `{text, replyer, bno}` JSON을 POST합니다.
3. 댓글 카드를 클릭하면 `rno`, `text`, `replyer`를 모달 입력창에 채웁니다.
4. Modify는 PUT, Remove는 DELETE를 호출합니다.
5. 성공 callback에서 댓글 목록을 다시 조회해 화면을 갱신합니다.

서버 렌더링 게시글과 비동기 댓글을 한 화면에 섞은 것이 이 프로젝트의 핵심 학습 포인트입니다.

## 7. 화면과 폼의 hidden 값

`list.html`의 게시글 링크에는 `bno`, `page`, `keyword`, `type`이 함께 들어갑니다.
`modify.html`은 이 값을 hidden input으로 유지한 뒤 수정/목록 버튼의 form action을 바꿉니다.
이 방식은 사용자가 검색한 3페이지의 게시글을 수정하고 다시 1페이지로 튕기지 않도록 하는 장치입니다.

## 8. 테스트 읽기 순서

- `BoardRepositoryTests.insertBoard`: Member와 Board 저장
- `testRead`: 게시글 단건 조회
- `testReadWriter`: 게시글/작성자 조합 조회
- `testReadReply`: 게시글/댓글 조합 조회
- `testWithReplyCount`: 목록용 댓글 수 집계
- `ReplyRepositoryTests.testListByBoard`: 특정 Board 댓글 목록
- `BoardServiceTest.testList`: 페이지 DTO 변환
- `BoardServiceTest.modify`: Entity 변경 메서드와 save 확인

테스트의 더미 INSERT/DELETE는 실제 MariaDB에 영향을 주므로 선택 실행해야 합니다.

## 다시 공부할 질문

1. `Board.writer`와 `Reply.board`가 LAZY인 이유는 무엇인가?
2. `Object[]` 반환 쿼리보다 DTO projection이 안전한 이유는 무엇인가?
3. 댓글을 먼저 삭제하지 않고 Board를 삭제하면 어떤 문제가 생기는가?
4. `@RestController`와 `@Controller`의 반환값 해석은 어떻게 다른가?
5. 댓글 수정에서 `PUT`, 삭제에서 `DELETE`를 선택한 이유는 무엇인가?
6. `replyCount`를 목록마다 별도 조회하면 N+1 문제가 어떻게 생기는가?
