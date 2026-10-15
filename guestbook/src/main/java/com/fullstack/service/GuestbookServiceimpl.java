package com.fullstack.service;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.fullstack.dto.GuestbookDTO;
import com.fullstack.dto.PageRequestDTO;
import com.fullstack.dto.PageResultDTO;
import com.fullstack.entity.Guestbook;
import com.fullstack.entity.QGuestbook;
import com.fullstack.repository.GuestbookRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
// guestbookRepository 이 새끼 final이라 무조건 초기화 해야한다. Requ... 이거 쓰면 알아서 해줌
public class GuestbookServiceimpl implements GuestbookService {
	private final GuestbookRepository guestbookRepository;
	
	// 검색어 처리를 위한 메서드 정의
	public BooleanBuilder getSearch(PageRequestDTO requestDTO) {
		String type = requestDTO.getType();
		BooleanBuilder booleanBuilder = new BooleanBuilder();
		QGuestbook qGuestbook = QGuestbook.guestbook;
		String keyword = requestDTO.getKeyword();
		
		// 글 전체에서 조건 검색을 할 예정, 글 번호 조건만 먼저 정의
		BooleanExpression expression = qGuestbook.gno.gt(0L); // 모든 글번호를 대상으로 범위 지정
		booleanBuilder.and(expression);
		
		// 검색 조건이 없는 경우 기본 넘겨줌
		if(type == null || type.trim().length() == 0) {
			return booleanBuilder;
		}
		
		// 세부 검색 조건을 or 로 묶어주기
		BooleanBuilder conditionBuilder = new BooleanBuilder();
		if(type.contains("t")) // 제목에서 검색이라면
			conditionBuilder.or(qGuestbook.title.contains(keyword));
		if(type.contains("c")) // 내용에서 검색이라면
			conditionBuilder.or(qGuestbook.content.contains(keyword));
		if(type.contains("w")) // 작성자에서 검색이라면
			conditionBuilder.or(qGuestbook.writer.contains(keyword));
		
		booleanBuilder.and(conditionBuilder);
		return booleanBuilder;
	}
	
	@Override
	public Long register(GuestbookDTO dto) {
		log.info("DTO------");
		log.info(dto);
		
		// Entity DTO 변환
		Guestbook guestbook = dtoToEntity(dto);
		log.info(guestbook);
		
		guestbookRepository.save(guestbook);
		
		return guestbook.getGno();
	}

	// list 페이지에 뿌려질 메서드 정의
	@Override
	public PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO pageRequestDTO) {
		Pageable pageable = pageRequestDTO.getPageable(Sort.by("gno").descending());
		
		BooleanBuilder booleanBuilder = getSearch(pageRequestDTO);
		
		Page<Guestbook> result = guestbookRepository.findAll(booleanBuilder, pageable);
		// findAll에 저렇게 booleanBuilder 넣으면 api가 알아서 검색 처리 해줌
		
		// Function 객체 생성
		Function<Guestbook, GuestbookDTO> fn = (entity -> entityToDto(entity));
		return new PageResultDTO<>(result, fn);
	}

	public GuestbookDTO read(Long gno) {
		// JPA를 이용해서 gno에 해당하는 Entity를 추출
		// Entity -> DTO 변환 후 리턴
		Optional<Guestbook> result = guestbookRepository.findById(gno);
		return result.isPresent()?entityToDto(result.get()) : null;
	}

	@Override
	public void modify(GuestbookDTO guestbookDTO) {
		// 업데이트 되는 항목은 제먹과 내용만
		Optional<Guestbook> result = guestbookRepository.findById(guestbookDTO.getGno());
		if(result.isPresent()) {
			Guestbook entity = result.get();
			
			// 엔티티에 수정할 내용 set
			entity.changeContent(guestbookDTO.getContent());
			entity.changeTitle(guestbookDTO.getTitle());
			
			guestbookRepository.save(entity);
		}
	}

	@Override
	public void remove(Long gno) {
		guestbookRepository.deleteById(gno);
	}
	
	
}
