package com.fullstack.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fullstack.dto.ReplyDTO;
import com.fullstack.entity.Board;
import com.fullstack.entity.Reply;
import com.fullstack.repository.ReplyRepository;
import com.fullstack.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/replies/")
@Log4j2
@RequiredArgsConstructor
public class ReplyController {
	private final ReplyService replyService;
	
	@GetMapping(value = "/board/{bno}", produces = MediaType.APPLICATION_JSON_VALUE)
	// /board/95 형식처럼 Path에 bno의 파라미터 값이 들어온다
	// produces 쪽은 이 서버가 클라이언트에 응답할 때 보내는 데이터의 형식이 JSON 타입을 명시
	public ResponseEntity<List<ReplyDTO>> getListByBoard(@PathVariable("bno") Long bno) {
		// @PathVariable("bno")는 위에 {bno} 이 자리에 들어온 값을 Long 타입의 bno 변수로 받아 들인다
		
		// ResponseEntity 는 단순히 데이터만 보내는 것이 아니라,
		// HTTP 상태 코드(Status Code)와 응답 헤더 및 본문(Body)을 함께 제어해서 보낼 수 있도록 감싸주는
		// Spring의 객체
		log.info("요청 bno ---> " + bno);
		return new ResponseEntity<List<ReplyDTO>>(replyService.getList(bno), HttpStatus.OK);
		// HttpStatus.OK는 요청이 성공적으로 처리되었다는 뜻
	}
	
	// 댓글 동록 처리
	@PostMapping("")
	public ResponseEntity<Long> register(@RequestBody ReplyDTO replyDTO){
		log.info("======================" + replyDTO);
		// 동록작업 정의
		Long rno = replyService.register(replyDTO);
		return new ResponseEntity<Long>(rno, HttpStatus.OK);
	}
	
	@DeleteMapping("/{rno}")
	public ResponseEntity<String> remove(@PathVariable("rno") Long rno){
		replyService.remove(rno);
		return new ResponseEntity<String>("success",HttpStatus.OK);
	}
	
	@PutMapping("/{rno}")
	public ResponseEntity<String> modify(@PathVariable("rno") Long rno,
			@RequestBody ReplyDTO replyDTO){
		replyDTO.setRno(rno); 
		replyService.modify(replyDTO);
		return new ResponseEntity<String>("success", HttpStatus.OK);
	}
}
