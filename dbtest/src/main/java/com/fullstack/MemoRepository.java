package com.fullstack;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fullstack.entity.Memo;

import jakarta.transaction.Transactional;

//제네릭은 <해당 엔티티, 해당 엔티티의 기본키 타입>
public interface MemoRepository extends JpaRepository<Memo, Long> {
	/*
	 * 메서드 만으로 쿼리를 실행 할 수 있다
	 * insert : save(Entity)
	 * select : findById(key)
	 * update : save(Entity) : 엔티티의 변경된 속성을 스스로 판단해서 update
	 * delete : deleteById(key), delete(entity)
	 */
	
	// 쿼리 메서드를 이용한 데이터 추출 예시 (잘 안쓰는 거)
	// findby~~
	List<Memo> findByMnoBetweenOrderByMnoDesc(long from, long to);
	
	// JPQL : @Query 어노테이션을 이용한 쿼리진행 (가장 많이 씀)
	// 반드시 테ㅣ블의 alias 를 이용해야 한다
	// dbms 에서는 테이블명이 tbl_memo 이지면 무조건 엔티티명을 써야함 아래의 Memo 처럼
	@Query("select m from Memo m order by m.mno desc")
	List<Memo> getListDesc();
	
	// 파라미터가 존재하는 쿼리
	@Transactional
	@Modifying
	// update 사용 할 때는 무조건 위에 2개 어노테이션 있어야함
	@Query("update Memo m set m.memoText = :memoText where m.mno = :mno")
	int updateMemoText(@Param("mno") long mno, @Param("memoText") String memoText);
	// @Param("mno") -> :mno에 long mno 이 파라미터가 들어감
	
	@Transactional
	@Modifying
	@Query
	("update Memo m set m.memoText = :#{#param.memoText} where m.mno = :#{#param.mno}")
	int updateMemoTextWithBean (@Param("param") Memo memo);
	// 이런 식으로 #param 이 친구가 @Param("param") 여기 안에 들어가야함 이런 방식도 가능
	
	// 페이징 처리가 되는 JPQL을 생성, Count 쿼리를 반드시 작성해야한다
	@Query(value = "select m from Memo m where m.mno > :mno",
			countQuery = "select count(m) from Memo m where m.mno > :mno")
	Page<Memo> getListWithQuery(@Param("mno") long mno, Pageable pageable);
	
	// 쿼리의 내용이 Entity 타입이 아닌 경우
	// 조회된 내용의 결과에 매칭되는 Entity 가 없을 경우에 사용
	// Object[]로 리턴을 받도록 한다
	@Query(value = "select m.mno, m.memoText, Current_Date from "
			+ "Memo m where m.mno > :mno",
			countQuery = "select count(m) from Memo m where m.mno > :mno")
	Page<Object[]> getListWithQueryObject(@Param("mno") long mno, Pageable pageable);
	
	// native 쿼리 : 엔티티를 사용하지 않고 일반 쿼리문 직접 전송하는 쿼리
	// JSON 등의 데이터를 전송하기 위해 사용된다
	// 위에 꺼들은 엔티티롤 사용해서 테이블명이 Memo 이지만 이건 아니라서 tbl_memo를 쓰는거임
	@Query(value = "select * from tbl_memo where mno > 0", nativeQuery = true)
	List<Object[]> getNativeResult();
	
}
