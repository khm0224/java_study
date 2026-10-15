# dbtest: Spring Data JPA Repository 실습

`dbtest`는 화면 없이 JPA Repository를 직접 호출하면서 데이터베이스 접근 방법을 비교하는 프로젝트입니다.
가장 먼저 이 프로젝트를 읽으면 `Entity -> Repository -> JPA/Hibernate -> SQL`의 관계를 확인하기 쉽습니다.

## 1. 의존성과 실행 설정

[`build.gradle`](./build.gradle)은 다음 기술을 사용합니다.

- `spring-boot-starter-data-jpa`: Jakarta Persistence API, Hibernate, Spring Data JPA
- `spring-boot-starter-webmvc`: Spring Boot 웹 실행 기반
- `mariadb-java-client`: MariaDB JDBC 드라이버
- Lombok: 반복적인 생성자/Getter/Builder 코드 생성
- `war`: 외부 Tomcat 배포용 WAR 구성을 연습

[`application.properties`](./src/main/resources/application.properties)는 MariaDB Driver, JDBC URL, 사용자, `ddl-auto=update`, SQL 출력 옵션을 설정합니다.

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.show-sql=true
```

`update`는 Entity 구조와 테이블을 비교해 필요한 DDL을 반영합니다. 학습 환경에서는 편하지만, 운영 데이터베이스의 스키마를 자동 변경하는 방식으로 사용하면 위험합니다. `show-sql=true`와 `format_sql=true`는 Hibernate가 실행하는 SQL을 콘솔에서 확인하는 학습용 설정입니다.

## 2. Entity: `Memo`

파일: `src/main/java/com/fullstack/entity/Memo.java`

```java
@Entity
@Table(name = "tbl_memo")
public class Memo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mno;

    @Column(length = 200, nullable = false)
    private String memoText;
}
```

이 클래스는 다음 SQL 구조와 대응합니다.

```sql
create table tbl_memo (
    mno bigint not null auto_increment,
    memo_text varchar(200) not null,
    primary key (mno)
);
```

- `@Entity`: JPA가 관리하는 Entity로 등록합니다.
- `@Table(name = "tbl_memo")`: 클래스명 `Memo`와 다른 실제 테이블명을 지정합니다.
- `@Id`: 모든 Entity가 가져야 하는 식별자입니다.
- `GenerationType.IDENTITY`: MariaDB의 auto increment가 번호를 생성합니다.
- Java 필드 `memoText`는 기본 naming 전략에 따라 `memo_text` 컬럼으로 변환됩니다.
- `nullable = false`: DB 컬럼에 NOT NULL 제약이 생깁니다.
- `@Builder`: 필드가 늘어나도 의미가 보이는 객체 생성을 할 수 있습니다.
- `@NoArgsConstructor`: JPA가 객체를 생성할 수 있는 기본 생성자입니다.
- `@AllArgsConstructor`: 모든 필드를 받는 생성자입니다.

## 3. `MemoRepository`의 기본 CRUD

파일: `src/main/java/com/fullstack/MemoRepository.java`

```java
public interface MemoRepository extends JpaRepository<Memo, Long> {
}
```

제네릭의 첫 번째는 Entity 타입, 두 번째는 기본키 타입입니다.

| 목적 | 메서드 | 반환/동작 |
|---|---|---|
| INSERT | `save(memo)` | 저장 후 생성된 `mno`가 Entity에 반영됨 |
| SELECT 1건 | `findById(100L)` | `Optional<Memo>` |
| UPDATE | `save(memo)` | 같은 식별자의 변경을 반영 |
| DELETE | `deleteById(102L)` | 식별자로 삭제 |
| 전체 조회 | `findAll()` | `List<Memo>` |
| 페이징 | `findAll(pageable)` | `Page<Memo>` |

`Optional`은 결과가 없을 수 있음을 타입으로 표현합니다.

```java
Optional<Memo> result = memoRepository.findById(mno);
if (result.isPresent()) {
    Memo memo = result.get();
}
```

실무에서는 `orElseThrow`, `ifPresent`, `map` 등을 사용해 존재하지 않는 경우를 명확히 처리합니다. `get()`만 호출하면 데이터가 없을 때 예외가 발생합니다.

## 4. 파생 쿼리 메서드

```java
List<Memo> findByMnoBetweenOrderByMnoDesc(long from, long to);
```

메서드 이름을 Spring Data가 분석하여 다음 의미로 해석합니다.

- `findBy`: 조회
- `MnoBetween`: `mno`가 두 값 사이
- `OrderByMnoDesc`: `mno` 내림차순

호출 예시는 `findByMnoBetweenOrderByMnoDesc(70L, 80L)`입니다. 간단한 조건에는 편하지만, 조건이 길어지거나 동적 검색이 필요하면 `@Query`나 QueryDSL이 더 읽기 좋습니다.

## 5. JPQL `@Query`

```java
@Query("select m from Memo m order by m.mno desc")
List<Memo> getListDesc();
```

JPQL은 테이블/컬럼이 아니라 Entity/필드 기준으로 작성합니다.

- `Memo`: 실제 테이블명 `tbl_memo`가 아니라 Entity명
- `m`: JPQL alias
- `m.mno`: Java Entity 필드
- 실제 SQL 변환은 Hibernate가 담당

### 파라미터 바인딩과 수정

```java
@Transactional
@Modifying
@Query("update Memo m set m.memoText = :memoText where m.mno = :mno")
int updateMemoText(@Param("mno") long mno,
                   @Param("memoText") String memoText);
```

- `:mno`, `:memoText`는 named parameter입니다.
- `@Param` 이름이 JPQL placeholder와 일치해야 합니다.
- `@Modifying`은 조회가 아닌 UPDATE/DELETE 쿼리임을 나타냅니다.
- `@Transactional`은 변경 쿼리를 트랜잭션 안에서 실행합니다.
- 반환값 `int`는 영향을 받은 행 수입니다.

Bean 하나를 파라미터로 넘기는 방식도 있습니다.

```java
@Query("""
    update Memo m
       set m.memoText = :#{#param.memoText}
     where m.mno = :#{#param.mno}
    """)
int updateMemoTextWithBean(@Param("param") Memo memo);
```

`#param`은 `@Param("param")`과 연결되고, SpEL 표현식으로 객체의 필드에 접근합니다.

## 6. JPQL 페이징과 `countQuery`

```java
@Query(
    value = "select m from Memo m where m.mno > :mno",
    countQuery = "select count(m) from Memo m where m.mno > :mno"
)
Page<Memo> getListWithQuery(@Param("mno") long mno, Pageable pageable);
```

`Page`를 만들려면 현재 페이지 데이터 조회 쿼리와 전체 개수를 계산하는 count 쿼리가 모두 필요합니다.
`PageRequest.of(0, 10, Sort.by("mno").descending())`를 넘기면 `mno > 1` 조건을 만족하는 데이터 중 첫 10건과 전체 페이지 계산 정보가 반환됩니다.

## 7. Entity가 아닌 결과: `Page<Object[]>`

```java
@Query(
    value = "select m.mno, m.memoText, Current_Date from Memo m where m.mno > :mno",
    countQuery = "select count(m) from Memo m where m.mno > :mno"
)
Page<Object[]> getListWithQueryObject(@Param("mno") long mno, Pageable pageable);
```

조회 컬럼 조합이 `Memo` Entity 하나와 일치하지 않으면 `Object[]`로 받습니다.
배열의 위치가 쿼리의 select 순서와 강하게 결합되므로, 규모가 커지면 DTO projection을 고려해야 합니다.

## 8. Native Query

```java
@Query(value = "select * from tbl_memo where mno > 0", nativeQuery = true)
List<Object[]> getNativeResult();
```

`nativeQuery = true`이면 JPQL이 아니라 MariaDB에 실제 SQL을 직접 전송합니다.

- 테이블명은 `tbl_memo`처럼 DB 이름을 씁니다.
- 컬럼명도 DB 컬럼명 기준입니다.
- DB 특화 함수나 이미 작성된 SQL을 사용해야 할 때 유용합니다.
- DBMS 변경 시 이식성이 낮아집니다.
- `Object[]` 결과는 인덱스 기반이라 타입/순서를 주의해야 합니다.

## 9. 테스트를 단계별로 읽기

파일: `src/test/java/com/fullstack/repository/MemoRepositoryTests.java`

### 데이터 생성

```java
IntStream.rangeClosed(1, 100).forEach(i -> {
    Memo memo = Memo.builder().memoText("Sample.." + i).build();
    memoRepository.save(memo);
});
```

`rangeClosed(1, 100)`은 1부터 100까지 포함합니다. Entity의 `mno`는 직접 넣지 않고 DB가 생성합니다.

### 페이징과 정렬

```java
Sort sort1 = Sort.by("mno").descending();
Sort sort2 = Sort.by("memoText").ascending();
Sort sortAll = sort1.and(sort2);
Pageable pageable = PageRequest.of(0, 10, sortAll);
Page<Memo> result = memoRepository.findAll(pageable);
```

`sort1`이 1차, `sort2`가 2차 정렬입니다. `Page`의 `getTotalPages`, `getTotalElements`, `getNumber`, `getSize`, `hasNext`, `isFirst`, `getContent`를 출력하며 페이지의 메타데이터와 실제 목록을 구분해서 확인합니다.

### `save()` 업데이트

```java
Memo memo = Memo.builder()
        .mno(100L)
        .memoText("Update 된 내용")
        .build();
memoRepository.save(memo);
```

식별자가 100인 행이 존재하면 update 대상으로 동작합니다. 이미 조회한 Entity를 수정하는 변경 감지 방식과, 식별자가 있는 객체를 `save`하는 방식의 차이를 SQL 로그로 비교해 보세요.

### 현재 활성화된 테스트

`getNative()`만 `@Test`가 활성화되어 있습니다. 나머지 실습 메서드는 주석 처리되어 있으므로, 실행 전 주석을 풀고 DB에 미치는 영향을 확인해야 합니다.

## 다시 공부할 때의 실습 순서

1. `ddl-auto=update`로 `tbl_memo`가 생성되는지 확인합니다.
2. `insertDummies()`를 실행하고 INSERT SQL을 관찰합니다.
3. `findById`, `findAll(Pageable)`의 반환 타입을 직접 출력합니다.
4. 파생 쿼리와 JPQL이 생성하는 SQL을 비교합니다.
5. `@Modifying` 없는 update 쿼리가 왜 실패하는지 확인합니다.
6. `countQuery`를 제거했을 때 페이징 계산이 어떻게 달라지는지 확인합니다.
7. JPQL과 Native Query에서 테이블명/필드명이 왜 다른지 정리합니다.
