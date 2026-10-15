package com.fullstack.service;

import com.fullstack.dto.GuestbookDTO;
import com.fullstack.dto.PageRequestDTO;
import com.fullstack.dto.PageResultDTO;
import com.fullstack.entity.Guestbook;

public interface GuestbookService {
	
	// 모디파이 메서드 선언
	void modify(GuestbookDTO guestbookDTO);
	
	// 삭제 메서드 선언
	void remove(Long gno);
	
	// 읽기 메서드 선언
	GuestbookDTO read(Long gno);
	
	// 게시글 등록 메서드 선언
	Long register(GuestbookDTO dto);

	PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO dto);
	
	// insert 하거나 update 할 때 사용됨
	// DTO --> Entity 로 변환하는 기본 메서드를 정의합니다.
	// 이렇게 하면 얘를 상속받은 클래스는 이 메서드를 사용할수 있겠지요.
	default Guestbook dtoToEntity(GuestbookDTO dto) {
		Guestbook entity = Guestbook.builder().gno(dto.getGno())
				.title(dto.getTitle()).content(dto.getContent())
				.writer(dto.getWriter()).build();
		return entity;
	}
	
	// 뷰어쪽에 보내줄 때 사용됨
	default GuestbookDTO entityToDto(Guestbook entity) {
	      GuestbookDTO dto = GuestbookDTO.builder()
	            .gno(entity.getGno()).title(entity.getTitle())
	            .content(entity.getContent()).writer(entity.getWriter())
	            .regDate(entity.getRegDate()).modDate(entity.getModDate()).build();
	      return dto;
	   }
}
