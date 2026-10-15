package com.fullstack.controller;

import com.fullstack.repository.GuestbookRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fullstack.dto.GuestbookDTO;
import com.fullstack.dto.PageRequestDTO;
import com.fullstack.service.GuestbookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/guestbook")
@Log4j2
@RequiredArgsConstructor
public class GuestbookController {
	private final GuestbookRepository guestbookRepository;
	// private final GuestbookService guestbookService = new GuestbookServiceimpl; 이게 들어가야하지만
	// @RequiredArgsConstructor 이거 쓰면 걍 아래처럼 하면 됨
	private final GuestbookService guestbookService;
	
	@GetMapping("/")
	public String index() {
		return "redirect:/guestbook/list";
	}
	
//	@GetMapping({"/","/list"}) // 1. /guestbook/ 2. /guestbook/list
//	public String list(PageRequestDTO pageRequestDTO, Model model) {
//		log.info("List 요청됨.." + pageRequestDTO);
//		return "/guestbook/list";
//	}
	
	// ========================================================================================
	// 페이지 정보) 클라이언트가 요청 -> 컨트롤러 -> 서버 계층 -> PageRequestDTO -> PageResultDTO -> 뷰
	// 기본 요청) 클라이언트 요청 -> 컨트롤러 -> 서버 계층 ->해당 기능dto -> 뷰
	
	// guestbookService는 인터페이스고 guestbookServiceimpl이 해당 인터페이스를 implements 받고 있음
	// guestbookRepository 안에 guestbookServiceimpl도 있는 상황이라
	// guestbookServiceimpl에 있는 오버로딩한 메서드를 사용할거면 guestbookService를 써야함
	// guestbookRepository가 하는건 DB와 직접 소통하며 데이터를 CRUD(생성, 조회, 수정, 삭제)하는 전용 창구
	// ========================================================================================
	
	@GetMapping("/list")
	public void list(PageRequestDTO pageRequestDTO, Model model) {
		log.info("List 요청됨.." + pageRequestDTO);
		
		// 요청된 페이지에 대한 정보를 service 계층에 파라미터로 넘기고
		// 리턴되는 페이지에 해당하는 정보를 담은 DTO List를 Model 계층에 저장
		model.addAttribute("result", guestbookService.getList(pageRequestDTO));
		
	}
	
	// 등록폼 요청 처리
	@GetMapping("/register")
	public void register() {
		
	}
	
	@PostMapping("register")
	public String registerPost(GuestbookDTO dto, RedirectAttributes ra) {
		// dto에 세팅된 내용을 service를 통해 DB에 등록하고
		// 리턴되는 글번호를 리다이엑트시 파라미터로 돌려 준다
		Long gno = guestbookService.register(dto);
		ra.addFlashAttribute("msg", gno);
		
		return "redirect:/guestbook/list";
	}
	
	// 상세 요청 처리 메서드 정의
	@GetMapping({"/read", "/modify"})
	public void read(@RequestParam("gno") Long gno,
			@ModelAttribute("requestDTO") PageRequestDTO dto, Model model) {
		// @ModelAttribute("requestDTO")는 read.html에 가버림
		// 그래서 read.html가보면 
		// <a th:href="@{/guestbook/modify(gno = ${dto.gno}, page=${requestDTO.page})}">
		// 여기에 page=${requestDTO.page}) 처럼 넘어가져서 사용가능한걸 알 수 있음
		GuestbookDTO guestbookDTO = guestbookService.read(gno);
		model.addAttribute("dto", guestbookDTO);
		
	}
	
	// Model : 하나의 HTTP 요청(Request) 안에서만 유지되기 때문에, 리다이렉트가 일어나 새로운 요청이 발생하면 담아둔 데이터가 전부 사라진다
	// RedirectAttributes : 리다이렉트 상황에서도 데이터가 유실되지 않도록 특별히 설계된 전용 택배 상자
	
	// addAttribute() — URL 쿼리 스트링으로 전달
	// 데이터를 URL 주소 뒤에 파라미터 형태로 붙여서 넘깁니다 (새로고침 등에서 계속 유지되어야 하는 값)
	
	// addFlashAttribute() — 세션을 이용한 일회성 데이터 전달
	// 데이터를 URL에 노출하지 않고, 세션에 잠깐 숨겨두었다가 리다이렉트된 페이지에서 딱 한 번 읽고 자동 소멸시키는 방식 (일회성 알림/팝업 메시지)
	
	// 수정 처리 요청 메서드 정의
	@PostMapping("/modify")
	public String modify(GuestbookDTO guestbookDTO, @ModelAttribute("requestDTO") PageRequestDTO requestDTO,
			RedirectAttributes ra) {
		guestbookService.modify(guestbookDTO);
		ra.addAttribute("page", requestDTO.getPage());
		ra.addAttribute("gno", guestbookDTO.getGno());
		ra.addAttribute("word", requestDTO.getKeyword());
		ra.addAttribute("keyword", requestDTO.getKeyword());
		// ra.addAttribute는 http://localhost:8080/guestbook/read?page=1&gno=299 이렇게 만들어 주기 위해서 파라미터에 뭐 들어가는지 함
		// 저 파라미터가 있어야 read -> list로 넘어갔을 때 첫페이지로 안가고 해당 튜플이 있는 페이지로 가게 만들어 줄 수 있음
		return "redirect:/guestbook/read";
	}
	
	// 삭제 요청 처리 메서드 정의
	@PostMapping("/remove")
	public String remove(@RequestParam("gno") Long gno, RedirectAttributes ra) {
		guestbookService.remove(gno);
		ra.addFlashAttribute("msg", gno);
		return "redirect:/guestbook/list";
	}
}
