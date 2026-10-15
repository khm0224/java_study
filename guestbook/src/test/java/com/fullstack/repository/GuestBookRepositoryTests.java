package com.fullstack.repository;

import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.fullstack.entity.Guestbook;
import com.fullstack.entity.QGuestbook;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;

@SpringBootTest
public class GuestBookRepositoryTests {
	
	@Autowired
	private GuestbookRepository guestbookRepository;
	
	//@Test
	public void insertDummies() {
		IntStream.rangeClosed(1, 300).forEach(i -> {
			Guestbook guestbook = Guestbook.builder().
					title("Title..."+i).content("Content.."+i)
					.writer("user" + (i%10)).build();
			System.out.println(guestbookRepository.save(guestbook));
		});
	}
	
	// 수정날자 테스트 해보기
	//@Test
	public void updateTest() {
		Optional<Guestbook> result = guestbookRepository.findById(300L);;
		
		if(result.isPresent()) {
			Guestbook guestbook = result.get();
			
			guestbook.changeTitle("변경된 제목");
			guestbook.changeContent("변경된 내용");
			guestbookRepository.save(guestbook);
			
		}
	}
	
	//@Test
	public void testQueryDsl() {
		/* queryDSL 사용 법 : BooleanBuilder 생성 <- 조건들을 넣어주는 컨테이너
		 * 조건에 맞는 구문은 QueryDSL 에서 사용하는 Predicate 타입의 함수를 생성함..
		 * 생성된 BooleanBuilder 에 작성된 Predicate 을 추가 하고 실행함 */
		
		// 페이징
		Pageable pageable = PageRequest.of(0, 10, Sort.by("gno"));
		
		// Q 도메인 클래스 얻어내기, 자바 코드로 안전하게 SQL/JPQL 컬럼을 참조할 수 있게 해줌
		QGuestbook qGuestbook = QGuestbook.guestbook;
		
		String keyword = "1";
		
		// BooleanBuilder 생성
		// 여러 검색 조건(Predicate)을 동적으로 추가하고 조합할 수 있는 컨테이너 역할
		BooleanBuilder builder = new BooleanBuilder();
		
		// 생성된 도메인 클래스의 속성(컬럼)에서 메서드를 이용한 검색을 실행
		// SQL의 LIKE '%1%'와 유사하게, 제목에 "1"이 포함되어 있는지 보고 생성
		BooleanExpression extitle = qGuestbook.title.contains(keyword);
		// 생성된 조건을 빌더에 AND 연산으로 추가합니다
		//builder.and(extitle);
		
		BooleanExpression excontent = qGuestbook.title.contains(keyword);
		// 제목과 내용 둘 중 하나에 1이 들어가는지
		BooleanExpression exAll = extitle.or(excontent);
		builder.and(exAll);
		builder.and(qGuestbook.gno.gt(0L)); // gno > 0 이라는 소리
		
		// 조회
		Page<Guestbook> result = guestbookRepository.findAll(builder, pageable);
		// 출력
		result.stream().forEach(gb -> {
			System.out.println(gb);
		});
	}
}
