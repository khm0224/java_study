# guestbook: 계층 분리와 QueryDSL 검색

`guestbook`은 `dbtest`에서 배운 JPA 접근을 실제 웹 애플리케이션 형태로 확장한 프로젝트입니다.
Controller가 요청을 받고, Service가 Entity/DTO와 검색/페이징을 조정하고, Repository가 DB를 조회합니다.

## 1. 전체 요청 흐름

```text
GET /guestbook/list
 -> GuestbookController.list(PageRequestDTO, Model)
 -> GuestbookServiceimpl.getList(PageRequestDTO)
 -> getSearch(PageRequestDTO)
 -> GuestbookRepository.findAll(BooleanBuilder, Pageable)
 -> PageResultDTO<GuestbookDTO, Guestbook>
 -> templates/guestbook/list.html
```

등록은 다음 흐름입니다.

```text
POST /guestbook/register
 -> GuestbookController.registerPost(GuestbookDTO, RedirectAttributes)
 -> GuestbookServiceimpl.register(dto)
 -> dtoToEntity(dto)
 -> guestbookRepository.save(entity)
 -> redirect:/guestbook/list
```

## 2. Entity와 Auditing

### `Guestbook`

파일: `src/main/java/com/fullstack/entity/Guestbook.java`

- `gno`: `@Id`, `IDENTITY` 전략의 기본키
- `title`: 최대 100자, NOT NULL
- `content`: 최대 1500자, NOT NULL
- `writer`: 최대 50자, NOT NULL
- `changeTitle`, `changeContent`: Entity 내부 값 변경 메서드

setter 전체를 열지 않고 제목과 내용 변경 메서드만 제공하는 이유는 수정 가능한 필드를 제한하고 변경 의도를 드러내기 위해서입니다.

### `BaseEntity`

파일: `src/main/java/com/fullstack/entity/BaseEntity.java`

```java
@MappedSuperclass
@EntityListeners(value = {AuditingEntityListener.class})
public abstract class BaseEntity {
    @CreatedDate
    @Column(name = "regDate", updatable = false)
    private LocalDateTime regDate;

    @LastModifiedDate
    @Column(name = "moddate")
    private LocalDateTime modDate;
}
```

- `@MappedSuperclass`: 별도 테이블을 만들지 않고 자식 Entity에 컬럼을 물려줍니다.
- `@CreatedDate`: 처음 저장한 시각을 기록합니다.
- `@LastModifiedDate`: 저장/수정 시각을 기록합니다.
- `updatable = false`: 등록일은 이후 UPDATE에서 바뀌지 않게 합니다.
- `@EntityListeners`: JPA lifecycle 이벤트를 감사 리스너에 연결합니다.

[`GuestbookApplication`](./src/main/java/com/fullstack/GuestbookApplication.java)의 `@EnableJpaAuditing`이 켜져 있어야 실제 날짜가 자동으로 채워집니다.

## 3. DTO와 변환 메서드

파일: `src/main/java/com/fullstack/dto/GuestbookDTO.java`, `src/main/java/com/fullstack/service/GuestbookService.java`

`GuestbookDTO`는 `gno`, `title`, `content`, `writer`, `regDate`, `modDate`를 가집니다. 화면 폼의 필드와 조회 결과를 표현하지만 JPA Entity 자체는 아닙니다.

Service 인터페이스의 default 메서드가 변환 규칙을 공통으로 제공합니다.

```java
default Guestbook dtoToEntity(GuestbookDTO dto) {
    return Guestbook.builder()
            .gno(dto.getGno())
            .title(dto.getTitle())
            .content(dto.getContent())
            .writer(dto.getWriter())
            .build();
}

default GuestbookDTO entityToDto(Guestbook entity) {
    return GuestbookDTO.builder()
            .gno(entity.getGno())
            .title(entity.getTitle())
            .content(entity.getContent())
            .writer(entity.getWriter())
            .regDate(entity.getRegDate())
            .modDate(entity.getModDate())
            .build();
}
```

조회 Entity를 그대로 Model에 넣지 않고 DTO로 변환하면 영속 객체와 화면 모델을 분리할 수 있습니다.

## 4. `PageRequestDTO`

파일: `src/main/java/com/fullstack/dto/PageRequestDTO.java`

필드는 `page`, `size`, `type`, `keyword`입니다.

```java
public Pageable getPageable(Sort sort) {
    return PageRequest.of(page - 1, size, sort);
}
```

화면 페이지는 1부터 세므로 `page - 1`로 Spring Data의 0-based 페이지로 바꿉니다.
`page`가 0이 되지 않도록 생성자 기본값이 1인지 확인하고, 사용자가 잘못된 값을 보낼 수 있는 실서비스에서는 validation도 추가해야 합니다.

## 5. QueryDSL 검색

### Repository

파일: `src/main/java/com/fullstack/repository/GuestbookRepository.java`

```java
public interface GuestbookRepository
        extends JpaRepository<Guestbook, Long>,
                QuerydslPredicateExecutor<Guestbook> {
}
```

`QuerydslPredicateExecutor`를 추가하면 `BooleanBuilder`나 `Predicate`를 전달하는 `findAll(predicate, pageable)`을 사용할 수 있습니다.
Gradle은 `querydsl-jpa:5.0.0:jakarta`, `querydsl-apt`를 사용해 `QGuestbook`을 생성합니다.

### 조건 조립

파일: `src/main/java/com/fullstack/service/GuestbookServiceimpl.java`

```java
BooleanBuilder booleanBuilder = new BooleanBuilder();
QGuestbook qGuestbook = QGuestbook.guestbook;
```

`getSearch(PageRequestDTO requestDTO)`는 `type` 문자를 해석합니다.

- `t`: 제목 `qGuestbook.title.contains(keyword)`
- `c`: 내용 `qGuestbook.content.contains(keyword)`
- `w`: 작성자 `qGuestbook.writer.contains(keyword)`
- `tc`, `tw`, `cw`, `tcw`: 여러 조건을 OR로 묶습니다.
- 마지막에는 검색 조건과 `gno > 0` 조건 등을 BooleanBuilder에 추가합니다.

`contains(keyword)`는 SQL의 `LIKE '%keyword%'`와 비슷한 조건을 생성합니다. QueryDSL은 문자열을 직접 이어 붙이지 않고 Java 속성을 사용하므로 오타를 컴파일 단계에서 줄일 수 있습니다.

### 페이징 실행

```java
Pageable pageable = pageRequestDTO.getPageable(
        Sort.by("gno").descending()
);
Page<Guestbook> result =
        guestbookRepository.findAll(booleanBuilder, pageable);
```

결과를 `PageResultDTO<>(result, this::entityToDto)`로 감싸면 화면이 필요한 DTO 목록과 페이지 버튼 계산값을 함께 전달할 수 있습니다.

## 6. `PageResultDTO` 계산

파일: `src/main/java/com/fullstack/dto/PageResultDTO.java`

생성자는 `Page<EN>`과 `Function<EN, DTO>`를 받습니다.

- `result.getContent()`의 각 Entity를 `fn.apply()`로 DTO로 변환합니다.
- `totalPage`는 `result.getTotalPages()`입니다.
- 현재 화면 페이지는 `pageable.getPageNumber() + 1`입니다.
- `size`는 페이지 크기입니다.
- 페이지 버튼은 한 번에 10개 단위로 만듭니다.
- `start`, `end`로 버튼 범위를 계산합니다.
- `prev`는 `start > 1`, `next`는 `end < totalPage`일 때 true입니다.

예를 들어 현재 7페이지이고 버튼 단위가 10이면 1~10을 보여줍니다. 13페이지라면 11~20을 계산하되 실제 마지막 페이지가 13이면 `end=13`으로 줄입니다.

## 7. Controller 메서드별 역할

파일: `src/main/java/com/fullstack/controller/GuestbookController.java`

### `index()`

`GET /guestbook/` 요청을 받아 목록 URL로 보냅니다. 애플리케이션의 진입 URL과 실제 목록 URL을 분리한 것입니다.

### `list(PageRequestDTO, Model)`

Spring MVC가 쿼리 파라미터를 `PageRequestDTO`에 바인딩합니다.
Service의 `getList()` 결과를 `"result"` 이름으로 Model에 담고 `guestbook/list` 템플릿을 렌더링합니다.

### `register()`, `registerPost(...)`

GET은 입력 폼을 보여주고, POST는 `GuestbookDTO`를 받아 `register()`를 호출합니다.
저장된 `gno`를 `RedirectAttributes`에 담거나 로그로 확인한 뒤 목록으로 redirect합니다.

### `read(...)`

`GET /guestbook/read?gno=...`와 `/modify`를 한 메서드에서 처리합니다.
`@ModelAttribute("requestDTO")`로 페이지/검색 조건을 Model에 함께 넣어 두기 때문에, 상세 화면에서 목록으로 돌아갈 때 원래 검색 위치를 유지할 수 있습니다.

### `modify(...)`, `remove(...)`

수정은 DTO와 `requestDTO`를 받고 저장 후 원래 페이지로 redirect합니다.
삭제는 `gno`로 Service의 `remove()`를 호출한 다음 목록으로 돌아갑니다.

## 8. Thymeleaf 화면

- `register.html`: `th:action="@{/guestbook/register}"`, `th:method="post"`로 등록
- `list.html`: `th:each="dto : ${result.dtoList}"`로 행 반복
- `list.html`: `result.prev`, `result.next`, `result.pageList`로 페이지 버튼 출력
- `read.html`: 등록일/수정일을 `#temporals.format`으로 출력
- `modify.html`: 기존 값을 `th:value`, textarea 본문으로 채우고 hidden field로 페이지/검색 조건 유지
- `layout/basic.html`: Bootstrap, jQuery, sidebar, navbar를 fragment로 공통 사용

## 9. 테스트로 확인하는 학습 포인트

`GuestBookRepositoryTests`는 300개 더미 데이터 생성, Entity 수정과 Auditing 날짜, QueryDSL 검색을 보여줍니다.

```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("gno"));
QGuestbook qGuestbook = QGuestbook.guestbook;
BooleanBuilder builder = new BooleanBuilder();
builder.and(qGuestbook.title.contains(keyword)
        .or(qGuestbook.content.contains(keyword)));
Page<Guestbook> result =
        guestbookRepository.findAll(builder, pageable);
```

`ServiceTests.testList()`는 `page=1`, `size=10`, `type="tc"`, `keyword="2"`를 서비스에 전달해 화면용 결과가 만들어지는지 확인합니다.
더미 데이터 생성 테스트의 `@Test`를 활성화할 때는 중복 데이터가 쌓일 수 있으므로 한 번 실행한 뒤 다시 실행하지 않는 것이 좋습니다.

## 다시 공부할 때의 질문

1. `Guestbook`에 `@NoArgsConstructor`가 없으면 왜 JPA가 곤란한가?
2. `@EnableJpaAuditing`을 제거하면 `regDate`, `modDate`가 어떻게 되는가?
3. `PageRequestDTO.page`는 1부터인데 `PageRequest.of`는 왜 `page - 1`인가?
4. `tcw` 검색을 AND로 묶으면 결과가 왜 달라지는가?
5. QueryDSL `QGuestbook`은 언제 생성되고 어디에 생성되는가?
6. DTO를 사용하지 않고 Entity를 Thymeleaf에 직접 넘길 때 생기는 결합은 무엇인가?
