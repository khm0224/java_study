package com.fullstack.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.fullstack.MemoRepository;
import com.fullstack.entity.Memo;

// 테스트용 클래스임을 선언 해야함
@SpringBootTest
public class MemoRepositoryTests {
	// 인터페이스는 객체를 생성 하지 못 하기 때문에 컨테이너에서 구현 객체를 주입 받도록 요청 해야한다
	
	// 구현 객체를 주입 받는다. AOP
	@Autowired
	private MemoRepository memoRepository;
	
	//@Test
	public void testClass() {
		// JPARepository 구현 객체가 잘 주입되었는지 테스트
		System.out.println(memoRepository.getClass().getName());
	}
	
	// 비워있는 테이블에 데이터를 신규로 생성하는걸 insert 한다
	//@Test
	public void insertDummies() {
		/*
		 * Entity 객체 하나를 생성한다.. 당연히 필드의 값은 세팅되어야 한다
		 * 이후 JPARepository 구현 객체를 이용해서 테이블에 insert 한다
		 */
		
		// 아래의 코드를 실행하면 하나의 Memo 엔티티객체가 생성됨
		IntStream.rangeClosed(1, 100).forEach(i -> { // 1번 인덱스 ~ 100번 인덱스까지
			Memo memo = Memo.builder().memoText("Sample.." + i).build();
			// 생성된 Memo 객체를 JPARepository를 이용해서 DB에 insert 한다
			memoRepository.save(memo);
		});
	}
	
	//@Test
	public void testSelect() {
		// 특정 글번호에 해당하는 데이터를 select 한다
		Long mno = 100L;
		Optional<Memo> result = memoRepository.findById(mno);
		System.out.println("================================");
		// Optional 의 대표적 메서드 isPresent() : 객체가 존재 하는지의 판단 메서드
		if(result.isPresent()) {
			Memo memo = result.get();
			System.out.println(memo);
		}
	}
	
	//@Test
	public void UpdateTest() {
		/* update 순서
		 * 1. 먼저 select 를 하고, 이 데이터를 Entity 메모리에 담아놓는다
		 * 2. 이후 파라미터로 들어오는 엔티티의 id 를 조회해서 메모리에 담겨진 Entity 와 같은 값인지를 검증한다
		 * 3. 이후 틀린 값만 update 를 해주는 형태를 취한다, 같은 값들은 실행 안함(update 안함)
		 */
		Memo memo = Memo.builder().mno(100L).memoText("Update 된 내용").build();
		System.out.println(memoRepository.save(memo));
	}
	
	//@Test
	public void deleteTest() {
		Long mno = 102L;
		memoRepository.deleteById(mno);
	}
	
	// 페이징 처리를 위한 select 테스트
	/*
	 * 페이징 처리를 위한 객체가 몇 개 필요. 이중 Pageable 인터페이스를 주로 활용
	 * 이 객체는 PageRequest 객체의 static 메서드인 of() 를 통해서 얻어냄
	 * 이때 파라미터로는 시작페이지 번호(0부터 시작함), 한 페이지에서 보여줄 글 목록 갯수를
	 * 파라미터로 넘겨야함
	 * PageRequest.of(0,10) : 전체 글 목록을 조회해서 0 페이지를 10개의 글목록으로 채워서 리턴하라는 의미
	 * 
	 * 오버로딩된 메서드로는 정렬을 선택할 수 있도록 파라미터(sort 객체)를 줄수도 있다
	 */
	
	//@Test
	public void testPageDefault() {
		Sort sort1 = Sort.by("mno").descending();
		Sort sort2 = Sort.by("memoText").ascending();
		
		Sort sortAll = sort1.and(sort2); // 1순위로 sort1으로 정렬하고 2순위로 sort2로 정렬
		
		//Pageable pageable = PageRequest.of(0, 10, sort1); // 10페이지 만들고 0페이지부터 시작
		Pageable pageable = PageRequest.of(0, 10, sortAll);
		Page<Memo> result = memoRepository.findAll(pageable);
		System.out.println(result);
		
		// Page 객체는 페이징 설정에 필요한 정보를 담고 있는 다양한 메서드가 존재함
		System.out.println(result.getTotalPages()); // 총 페이지 수
		System.out.println(result.getTotalElements()); // 총 튜플 수
		System.out.println(result.getNumber()); // 첫 페이지 번호
		System.out.println(result.getSize()); // 페이지 사이즈
		System.out.println(result.hasNext()); // 다음이 있냐?
		System.out.println(result.isFirst()); // 처음이냐?
		
		for(Memo m : result.getContent()) { // 지금 페이지(0번 페이지)에 있는 튜플 출력
			System.out.println(m);
		}
	}
	
	//@Test
	public void testMethodQuery() {
		List<Memo> list = memoRepository.findByMnoBetweenOrderByMnoDesc(70L, 80L);
		list.forEach(i -> {
			System.out.println(i);
		});
	}
	
	//@Test
	public void jpqlTest() {
		List<Memo> result = memoRepository.getListDesc();
		result.forEach(i -> {
			System.out.println(i);
		});
	}
	
	//@Test
	public void jpqlUpdateTest() {
		int result = memoRepository.updateMemoText(98, "jpql로 수정");
	}
	
	//@Test
	public void jpqlUpdateTest2() {
		int result = memoRepository.updateMemoTextWithBean(
				Memo.builder().mno(99L).memoText("다른방식으로 수정").build());
	}
	
	//@Test
	public void jpqlSelectWithPage() {
		Sort sort1 = Sort.by("mno").descending();
		Pageable pageable = PageRequest.of(0, 10, sort1);
		Page<Memo> pg = memoRepository.getListWithQuery(1L, pageable);
		System.out.println(pg);
	}
	
	//@Test
	public void jpqlSelectObject() {
		Sort sort1 = Sort.by("mno").descending();
		Pageable pageable = PageRequest.of(0, 10, sort1);
		Page<Object[]> pg = memoRepository.getListWithQueryObject(1L, pageable);
		
		pg.forEach(obj -> {
			System.out.println(obj);
		});
	}
	
	@Test
	public void getNative() {
		List<Object[]> list = memoRepository.getNativeResult();
		list.forEach(obj -> System.out.println(obj));
	}
}
