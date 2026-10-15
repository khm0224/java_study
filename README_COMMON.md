# 공통 Spring 학습 노트

## 1. 애플리케이션 시작점

각 프로젝트의 `*Application` 클래스에는 `@SpringBootApplication`과 `main()`이 있습니다.

```java
@SpringBootApplication
public class GuestbookApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuestbookApplication.class, args);
    }
}
```

`@SpringBootApplication`은 다음 역할을 묶은 메타 어노테이션입니다.

- `@Configuration`: 이 클래스를 설정 클래스처럼 사용합니다.
- `@EnableAutoConfiguration`: 의존성과 설정을 보고 필요한 Spring Bean을 자동 구성합니다.
- `@ComponentScan`: 애플리케이션 클래스가 속한 패키지 이하에서 `@Controller`, `@Service`, `@Repository`, `@Component` 등을 찾아 Bean으로 등록합니다.

따라서 메인 클래스보다 상위 패키지에 컴포넌트를 두거나, 스캔 대상에서 벗어난 패키지에 클래스를 만들면 자동 주입되지 않을 수 있습니다.

## 2. 요청 처리의 기본 흐름

브라우저 요청은 대체로 다음 순서로 이동합니다.

```text
브라우저
  -> DispatcherServlet
  -> Controller
  -> Service
  -> Repository
  -> JPA/Hibernate
  -> MariaDB
  -> Entity/DTO 변환
  -> Model
  -> Thymeleaf HTML
```

### Controller

Controller는 URL과 HTTP 메서드를 Java 메서드에 연결합니다.

- `@Controller`: 반환값을 HTML 템플릿 이름으로 해석합니다.
- `@RequestMapping("/guestbook")`: 클래스 전체 URL 앞부분을 정합니다.
- `@GetMapping("/list")`: GET `/guestbook/list`를 연결합니다.
- `@PostMapping("/register")`: POST 등록 요청을 연결합니다.
- `@RequestParam("gno")`: 쿼리스트링 또는 요청 파라미터 하나를 받습니다.
- `@ModelAttribute("requestDTO")`: 폼/쿼리 파라미터를 객체에 바인딩하고 Model에도 같은 이름으로 넣습니다.
- `Model`: HTML에서 사용할 값을 담습니다.
- `RedirectAttributes`: POST 처리 후 redirect하면서 일회성 메시지나 파라미터를 전달합니다.

등록과 수정 후 바로 HTML을 반환하지 않고 `redirect:`를 사용하는 이유는 새로고침 시 POST가 반복되는 것을 막는 PRG(Post-Redirect-Get) 패턴을 적용하기 위해서입니다.

### Service

Service는 Controller가 데이터베이스 구현을 직접 알지 않게 하고, 여러 Repository 호출과 변환 로직을 한 곳에 둡니다.
`@Service`가 붙은 구현체는 Spring Bean이 되며, `@RequiredArgsConstructor`를 쓰면 `final` 필드 생성자가 자동 생성되어 생성자 주입이 됩니다.

### Repository

`JpaRepository<Entity, IdType>`를 상속하면 구현 클래스를 직접 만들지 않아도 Spring Data가 런타임에 구현체를 생성합니다.
대표 메서드는 다음과 같습니다.

- `save(entity)`: 새 Entity면 INSERT, 식별자가 있는 기존 Entity면 변경 감지/UPDATE에 사용됩니다.
- `findById(id)`: `Optional<Entity>`를 반환합니다.
- `findAll()`: 전체 목록을 반환합니다.
- `findAll(pageable)`: `Page<Entity>`를 반환합니다.
- `deleteById(id)`, `delete(entity)`: 삭제합니다.

## 3. Entity와 DTO를 구분하는 이유

Entity는 데이터베이스 테이블과 영속성 컨텍스트에 연결된 객체입니다. DTO는 화면이나 HTTP 요청/응답에 사용하는 데이터 묶음입니다.

- Entity를 화면에 바로 노출하면 DB 구조와 API 구조가 강하게 결합됩니다.
- DTO에는 화면 전용 필드, 검색 조건, 페이징 정보 등을 넣을 수 있습니다.
- Service에서 `dtoToEntity()`, `entityToDto()`를 통해 경계를 명확히 합니다.
- Entity의 변경은 `changeTitle()`처럼 의도를 드러내는 메서드로 제한하는 편이 안전합니다.

## 4. JPA 영속성 핵심

`@Entity` 클래스는 기본 생성자가 필요합니다. 이 저장소에서는 Lombok의 `@NoArgsConstructor`로 제공합니다.

- `@Id`: 기본키입니다.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: MariaDB AUTO_INCREMENT에 맡깁니다.
- `@Column(nullable = false, length = ...)`: 컬럼 제약과 길이를 지정합니다.
- `@Table(name = "...")`: 테이블명을 명시합니다.
- `@MappedSuperclass`: 공통 필드를 자식 Entity 테이블에 물려줍니다.

`save()`로 update할 때 JPA는 식별자로 기존 데이터를 찾고, 영속 상태 Entity의 값이 바뀌었는지 비교해 변경된 컬럼 중심으로 UPDATE를 수행합니다. 단순히 새 객체를 만드는 것과 이미 조회한 객체를 수정하는 것은 영속성 관점에서 다릅니다.

## 5. 페이징

Spring Data의 페이지 번호는 0부터 시작합니다.

```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("gno").descending());
Page<Guestbook> result = repository.findAll(pageable);
```

- `PageRequest.of(page, size)`: `page`는 0부터, `size`는 한 페이지의 행 수입니다.
- `Sort.by("gno").descending()`: Entity 필드명 기준 정렬입니다.
- `Page#getContent()`: 현재 페이지 데이터입니다.
- `getTotalPages()`: 전체 페이지 수입니다.
- `getTotalElements()`: 전체 행 수입니다.
- `getNumber()`: 현재 페이지(0부터)입니다.
- `hasNext()`, `isFirst()`: 버튼 표시 판단에 사용합니다.

화면에는 보통 사용자가 보는 1부터 시작하는 페이지 번호를 전달하므로 `PageResultDTO`에서 `pageable.getPageNumber() + 1`을 사용합니다.

## 6. Thymeleaf

`src/main/resources/templates`의 HTML은 서버에서 렌더링됩니다.

- `th:text`: HTML 텍스트를 출력합니다.
- `th:value`: input value를 채웁니다.
- `th:each`: 목록을 반복합니다.
- `th:if`: 조건부 출력입니다.
- `th:href`, `th:action`: 서버 URL을 생성합니다.
- `th:replace`: layout fragment를 재사용합니다.
- `${#temporals.format(...)}`: `LocalDateTime`을 화면 형식으로 바꿉니다.

`layout/basic.html`의 `setContent` fragment를 각 화면이 `th:replace`로 호출하는 구조는 공통 메뉴/Bootstrap을 반복 작성하지 않기 위한 레이아웃 패턴입니다.

## 7. Lombok을 읽는 법

- `@Getter`: getter 생성
- `@Data`: getter/setter, `toString`, `equals`, `hashCode` 등을 생성
- `@Builder`: `Memo.builder().memoText("...").build()` 형태의 객체 생성
- `@NoArgsConstructor`, `@AllArgsConstructor`: 생성자 생성
- `@RequiredArgsConstructor`: `final` 필드 생성자 생성
- `@ToString`: 디버깅용 문자열 생성
- `@Log4j2`: `log.info(...)`, `log.error(...)` 등을 사용할 수 있게 합니다.

Entity에 무분별한 `@Data`를 사용하면 setter가 모든 값을 열어둘 수 있으므로, 이 저장소처럼 `@Getter`와 변경 메서드를 조합하는 방식도 함께 익혀야 합니다.

## 8. 테스트 읽는 법

`@SpringBootTest`는 애플리케이션 컨텍스트를 올려 실제 Repository Bean과 설정을 사용합니다.
`@Autowired`는 컨테이너가 생성한 구현 객체를 주입합니다. Repository 인터페이스를 직접 `new`할 수 없는 이유가 여기에 있습니다.

테스트 클래스의 `//@Test`는 실습 시 선택적으로 실행하려고 주석 처리된 경우가 많습니다. 주석을 풀면 실제 DB 데이터가 INSERT/UPDATE/DELETE될 수 있으므로 대상 데이터베이스를 먼저 확인해야 합니다.
