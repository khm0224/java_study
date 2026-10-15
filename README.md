# Spring 학습 저장소

이 폴더는 Spring Boot와 Spring Data JPA를 공부하면서 만든 네 개의 작은 애플리케이션을 모아 둔 저장소입니다.
각 프로젝트는 하나의 완성된 서비스라기보다, 이전 프로젝트에서 배운 내용을 다음 프로젝트에서 확장하는 실습 단계에 가깝습니다.

## 문서 읽는 순서

1. [공통 개념과 실행 방법](./README_COMMON.md)
2. [dbtest - JPA Repository 기초](./README_DBTEST.md)
3. [guestbook - 계층 분리, DTO, 페이징, QueryDSL](./README_GUESTBOOK.md)
4. [board - 게시판, 회원, 답글, 비동기 요청](./README_BOARD.md)
5. [moivereview - 연관관계, 이미지 업로드, 리뷰](./README_MOVIEREVIEW.md)

`demo`, `thymeleaf` 등 다른 폴더는 현재 요청한 네 프로젝트의 핵심 학습 범위에서 제외했습니다.

## 프로젝트 비교

| 프로젝트 | 여기서 집중해서 볼 내용 |
|---|---|
| `dbtest` | `@Entity`, `JpaRepository`, CRUD, 파생 쿼리, JPQL, 수정 쿼리, 페이징, Native Query |
| `guestbook` | Controller-Service-Repository 구조, Entity/DTO 변환, JPA Auditing, QueryDSL 검색, 페이지 계산 |
| `board` | 게시글과 회원 연관관계, 답글 API, 화면과 JSON 통신, 수정/삭제와 검색 조건 유지 |
| `moivereview` | 영화-이미지-리뷰 모델링, 다중 파일 업로드, 썸네일, 리뷰 등록/수정/삭제 |

## 공통 실행 전 확인

- 네 프로젝트 모두 Java 21 Toolchain을 사용합니다.
- MariaDB의 `bootex` 데이터베이스와 프로젝트에 맞는 계정이 필요합니다.
- `src/main/resources/application.properties`의 데이터베이스 접속 정보는 각 프로젝트에서 확인해야 합니다.
- `ddl-auto=update`는 학습 중 테이블을 자동으로 맞추는 설정이지만, 운영 환경에서 무조건 사용하면 안 됩니다.
- 비밀번호와 실제 운영 접속 정보는 README나 Git에 기록하지 않습니다.

각 하위 README는 “이 클래스가 무엇인가”뿐 아니라 “왜 필요한가”, “호출 순서가 어떻게 되는가”, “메서드의 반환값을 다음 계층에서 어떻게 사용하는가”까지 코드와 연결해서 설명합니다.
