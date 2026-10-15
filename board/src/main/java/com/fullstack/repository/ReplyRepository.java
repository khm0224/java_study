package com.fullstack.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.fullstack.entity.Reply;
import com.fullstack.entity.Board;


public interface ReplyRepository extends JpaRepository<Reply, Long>{
	@Modifying // update, delete 쿼리 수행시에는 선언 해야함
	@Query("delete from Reply r where r.board.bno = :bno")
	void deleteByBno(@Param("bno") Long bno);
	
	// 게시물로 댓글 목록 가져오기 구현
	List<Reply> getRepliesByBoardOrderByRno(Board board);
}
