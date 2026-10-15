# moivereview: 영화, 이미지 업로드, 리뷰

`moivereview`는 네 프로젝트 중 가장 기능이 많습니다. 영화와 이미지의 1:N 관계, 영화와 회원/리뷰의 N:1 관계, AJAX multipart 업로드, 썸네일 생성, 평점 집계를 한 흐름으로 연결합니다.

## 1. 기술 스택과 설정

[`build.gradle`](./build.gradle)은 Spring Data JPA, MVC, Thymeleaf, MariaDB, Lombok과 `Thumbnailator 0.4.14`를 사용합니다.
`Thumbnailator`는 업로드한 원본 이미지로 작은 썸네일을 만들 때 사용합니다.

[`application.properties`](./src/main/resources/application.properties)의 파일 관련 설정은 다음 의미입니다.

```properties
spring.servlet.multipart.enabled=true
spring.servlet.multipart.location=C:\\upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=30MB
com.fullstack.upload.path=C:\\upload
```

- multipart 요청 활성화
- 임시 저장 위치 `C:\upload`
- 한 파일 최대 10MB
- 한 요청 전체 최대 30MB
- 애플리케이션이 최종 파일을 저장할 경로

실행 전에 해당 폴더 권한과 디스크 공간을 확인해야 합니다. 운영에서는 원본 파일명 검증, 확장자/Content-Type 검증, 경로 조작 방지, 인증/인가, 저장소 분리를 추가해야 합니다.

## 2. Entity 연관관계

```text
Movie 1 ---- N MovieImage
Movie 1 ---- N Review N ---- 1 Member
```

### `Movie`

`mno`, `title`, `regDate`, `modDate`를 가집니다. 이미지와 리뷰는 별도 테이블에 저장하고 Movie를 `@ManyToOne(fetch = LAZY)`로 참조합니다.

### `MovieImage`

필드는 `ino`, `uuid`, `imgName`, `path`, `movie`입니다.

- `uuid`: 같은 파일명이 겹치지 않게 만드는 식별자
- `imgName`: 사용자가 올린 원본 파일명
- `path`: 날짜 기반 디렉터리
- `movie`: 여러 이미지가 하나의 영화에 연결되는 LAZY 연관관계

`@ToString(exclude = "movie")`는 MovieImage를 출력할 때 Movie가 다시 이미지를 출력하는 순환 참조를 방지합니다.

### `Member`

`m_member` 테이블에 `mid`, `email`, `pw`, `nickname`을 저장합니다. 현재 프로젝트에서는 리뷰 작성자 참조를 위한 학습 모델이며, 완성된 인증/비밀번호 암호화 시스템은 아닙니다.

### `Review`

`reviewnum`, `movie`, `member`, `grade`, `text`를 가집니다.
`changGrade(int)`, `changeText(String)`로 변경 가능한 값을 제한합니다. Movie와 Member 모두 LAZY `@ManyToOne`입니다.

모든 공통 날짜는 `BaseEntity`의 `@CreatedDate`, `@LastModifiedDate`와 `@EnableJpaAuditing`으로 자동 관리합니다.

## 3. DTO

### `MovieDTO`

`mno`, `title`, `imageDTOList`, `avg`, `reviewCnt`, `regDate`, `modDate`를 가집니다.
목록/상세 화면이 필요한 이미지 목록과 평균 평점/리뷰 수까지 한 객체로 전달합니다.
`@Builder.Default`의 빈 `imageDTOList` 덕분에 이미지가 없는 경우에도 null 대신 빈 목록을 사용할 수 있습니다.

### `MovieImageDTO`

`ino`, `imgName`, `path`, `uuid`를 가지고 다음 메서드를 제공합니다.

```java
getImageURL()
getThumbnailURL()
```

두 메서드는 날짜 경로, UUID, 파일명을 조합해 원본 또는 `s_` 접두사가 붙은 썸네일 URL을 만듭니다. 화면에서 `/display?fileName=...` 형태로 이미지를 요청할 때 사용합니다.

### `ReviewDTO`

`reviewnum`, `mno`, `mid`, `nickname`, `grade`, `text`, `regDate`, `modDate`를 갖습니다.
Entity의 Movie/Member 연관 객체를 화면용 식별자와 닉네임으로 평탄화합니다.

### `UploadResultDTO`

업로드 직후 브라우저에 반환하는 `uuid`, `imgName`, `path`와 이미지 URL/썸네일 URL 생성 메서드를 제공합니다.
최종 영화 등록 POST는 이 DTO에서 만든 hidden input들을 `imageDTOList[...]` 이름으로 다시 서버에 보냅니다.

## 4. Repository 쿼리

### `MovieRepository.getListPage(Pageable)`

영화 목록을 페이지 단위로 조회하고 이미지 및 리뷰 통계에 필요한 데이터를 조합합니다. 평균 평점과 리뷰 수를 `Object[]`로 받은 뒤 `MovieServiceImpl.entityToDto()`에서 `MovieDTO`로 변환합니다.

### `MovieRepository.getMovieWithAll(Long mno)`

영화 하나, 이미지 목록, 리뷰/평점 집계 또는 관련 데이터를 상세 화면용으로 가져오는 조회입니다. `Object[]`의 각 위치를 서비스가 정해진 순서로 해석하므로 Repository 쿼리와 서비스 캐스팅 순서를 함께 읽어야 합니다.

### `MovieimageRepository`

영화 번호로 이미지 목록을 찾거나 영화에 연결된 이미지를 삭제하는 역할을 합니다. 영화 삭제/수정 시 DB의 이미지 행과 실제 디스크 파일을 모두 정리해야 한다는 점을 구분해서 봅니다.

### `ReviewRepository`

- `findByMovie(Movie)`: 특정 영화 리뷰 목록
- `deleteByMember(Member)`: 회원 기준 리뷰 삭제

리뷰 삭제 쿼리가 수정 쿼리라면 `@Modifying`과 트랜잭션이 필요합니다.

## 5. `MovieServiceImpl`의 변환과 등록

### `dtoToEntity(MovieDTO)`

DTO의 제목을 Movie Entity로 변환합니다. 이미지 DTO 자체는 Entity가 아니므로 Movie를 먼저 저장한 뒤 각 `MovieImage`에 저장한 Movie를 연결합니다.

### `register(MovieDTO)`

1. MovieDTO를 Movie Entity로 변환
2. `movieRepository.save(movie)`로 영화 저장
3. 생성된 `mno`를 확보
4. `imageDTOList`를 순회
5. 각 DTO를 MovieImage Entity로 바꾸고 Movie 연결
6. `movieimageRepository.save(image)` 실행
7. `mno` 반환

이 순서가 필요한 이유는 이미지의 외래키가 먼저 저장된 Movie를 참조하기 때문입니다. 현재 코드가 이미지가 반드시 하나 이상 있다고 가정하는지 확인하고, 실제 서비스에서는 빈 파일 목록을 명확히 처리해야 합니다.

### `entityToDto(...)`

조회된 Movie/이미지/평점/리뷰 수를 다음으로 변환합니다.

- Movie의 기본 필드
- 이미지 Entity 목록을 `MovieImageDTO` 목록으로 변환
- 평균 평점 `avg`
- 리뷰 수 `reviewCnt`
- 등록/수정일

### `getList(PageRequestDTO)`

정렬 조건을 포함한 Pageable을 만들고 `getListPage`를 호출한 뒤 `PageResultDTO`로 포장합니다.
페이지 번호는 화면 1-based, Spring Data 0-based 규칙을 그대로 따릅니다.

### `getMovie(Long mno)`

영화 상세용 join 결과를 읽어 MovieDTO로 변환합니다. 이미지가 여러 개일 때 중복 Movie 행이 생길 수 있으므로 쿼리와 변환 코드가 중복을 어떻게 처리하는지 확인해야 합니다.

## 6. Controller 요청 흐름

파일: `src/main/java/com/fullstack/controller/MoiveController.java`

| 요청 | 역할 |
|---|---|
| `GET /movie/` | 목록으로 이동 |
| `GET /movie/list` | 영화 목록/페이지 |
| `GET /movie/register` | 등록 화면 |
| `POST /movie/register` | 제목과 업로드 결과를 저장 |
| `GET /movie/read?mno=...` | 상세 화면 |

`MoiveController`라는 클래스명은 Movie의 오타이지만 Spring Bean 동작에는 클래스명이 필수 URL이 아니므로 매핑 자체는 가능합니다.

## 7. 파일 업로드 Controller

파일: `src/main/java/com/fullstack/controller/UploadController.java`

### `GET /display`

저장 경로를 `fileName`으로 받아 원본 또는 썸네일 파일을 `Resource`로 읽어 응답합니다.
브라우저의 `<img src="/display?fileName=...">`가 이 메서드를 호출합니다.

### `POST /upload`

`MultipartFile[] uploadFiles`를 받아 각 파일을 처리하고 `List<UploadResultDTO>`를 JSON으로 반환합니다.

일반적인 처리 순서는 다음과 같습니다.

1. 업로드 파일이 비어 있는지 확인
2. 원본 파일명 추출
3. UUID 생성
4. 날짜 폴더 생성
5. UUID와 원본 확장자를 합친 저장 파일명 생성
6. 원본 저장
7. Thumbnailator로 `s_` 썸네일 생성
8. 경로/파일명을 UploadResultDTO로 반환

### `GET /uploadTest`, `POST /removeFile`

`UploadTestController`와 `UploadController`는 업로드를 독립적으로 시험할 수 있는 화면/삭제 endpoint를 제공합니다.
삭제 시 원본과 썸네일을 각각 지워야 하며, 파일이 없을 때의 예외 처리를 확인해야 합니다.

## 8. 브라우저의 multipart 흐름

`movie/register.html`은 `<input type="file" multiple>`을 사용합니다.

```javascript
var formData = new FormData();
var files = inputFile[0].files;
for (var i = 0; i < files.length; i++) {
    formData.append("uploadFiles", files[i]);
}
```

AJAX 요청에 `contentType: false`, `processData: false`를 주어 FormData가 브라우저의 multipart 형식으로 전송되게 합니다.
응답으로 받은 각 업로드 결과는 미리보기 이미지와 hidden input으로 화면에 추가됩니다.

최종 영화 등록 시 hidden input 이름을 다음처럼 만들면 Spring MVC가 `MovieDTO.imageDTOList`에 자동 바인딩합니다.

```text
imageDTOList[0].uuid
imageDTOList[0].imgName
imageDTOList[0].path
```

업로드와 영화 저장이 분리되어 있으므로, 업로드 후 최종 등록을 취소하면 디스크에 고아 파일이 남을 수 있습니다. 완성 서비스라면 임시 업로드 정리 정책이 필요합니다.

## 9. 목록과 상세 화면

### `movie/list.html`

- `result.dtoList` 반복
- 첫 이미지가 있으면 `getThumbnailURL()`로 썸네일 출력
- 등록일 포맷 출력
- 평균 평점/리뷰 수 표시
- `result.prev`, `pageList`, `result.next`로 페이지 이동

### `movie/read.html`

영화 제목, 원본/썸네일 이미지, 평점, 리뷰 목록을 보여줍니다.
`starrr.js`로 별점 UI를 만들고 jQuery AJAX로 리뷰를 처리합니다.

리뷰 요청 데이터 예시는 다음과 같습니다.

```javascript
{
    mno: mno,
    grade: grade,
    text: inputText.val(),
    mid: inputMid.val()
}
```

리뷰 목록 조회, 등록, 수정, 삭제의 URL과 HTTP 메서드는 `ReviewController`가 아니라 `MoiveController`/관련 서비스의 매핑을 실제 코드와 함께 확인해야 합니다. 브라우저 AJAX의 `reviewnum`, `mno`, `mid`가 DTO 필드와 정확히 대응해야 합니다.

## 10. 리뷰 Service

### `ReviewService`

- `register(ReviewDTO)`: 리뷰 저장 후 번호 반환
- `getList(Long mno)`: 영화별 리뷰 목록
- `modify(ReviewDTO)`: 평점/내용 변경
- `remove(Long reviewnum)`: 리뷰 삭제
- `dtoToEntity`, `entityToDto`: 경계 변환

등록 시 Movie와 Member를 식별자로 연결하고, 수정 시 `changGrade`, `changeText`만 호출합니다.
삭제는 URL의 reviewnum으로 대상 Entity를 찾은 뒤 Repository 삭제를 수행합니다.

## 11. 테스트 읽기 순서

- `MovieRepositoryTests.insertdummies`: 영화와 이미지 더미 저장
- `testListPage`: 이미지/평점/리뷰 수가 포함된 페이지 조회
- `testGetMovieWithAll`: 상세 join 결과 확인
- `ReviewRepositoryTests.insertDummies`: 영화/회원/리뷰 데이터 생성
- `testGetMovieReview`: 영화별 리뷰 조회
- `testDeleteMember`: 회원 관련 리뷰 삭제 쿼리
- `MemberRepositoryTests.insertDummies`: 회원 데이터 생성

파일 업로드는 단순 Repository 테스트와 달리 실제 `C:\upload`에 파일을 만들 수 있으므로 테스트 후 파일을 정리해야 합니다.

## 다시 공부할 질문

1. Movie를 먼저 저장하지 않고 MovieImage를 저장할 수 없는 이유는 무엇인가?
2. DB의 이미지 행 삭제와 디스크 이미지 삭제를 둘 다 해야 하는 이유는 무엇인가?
3. 원본 파일명만 저장하면 파일명이 충돌하는 이유는 무엇인가?
4. `FormData` 요청에서 `processData: false`가 필요한 이유는 무엇인가?
5. `MovieImageDTO.getThumbnailURL()`이 원본 URL과 다른 파일명을 만드는 규칙은 무엇인가?
6. 목록 조회에서 이미지와 리뷰를 한 번에 join할 때 중복 행이 생길 수 있는 이유는 무엇인가?
7. 업로드 후 영화 등록을 취소했을 때 고아 파일을 어떻게 정리할 것인가?
