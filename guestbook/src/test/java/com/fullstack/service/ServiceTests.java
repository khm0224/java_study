package com.fullstack.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fullstack.dto.GuestbookDTO;
import com.fullstack.dto.PageRequestDTO;
import com.fullstack.dto.PageResultDTO;
import com.fullstack.entity.Guestbook;

@SpringBootTest
public class ServiceTests {
	@Autowired
	private GuestbookService guestbookService;
	
	//@Test
	public void testRegister() {
		GuestbookDTO dto = GuestbookDTO.builder().title("Sample Title..")
				.content("샘플 내용").writer("user0").build();
		System.out.println(guestbookService.register(dto));
	}
	
	@Test
	public void testList() {
		PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
				.page(1).size(10).type("tc").keyword("2").build();
		PageResultDTO<GuestbookDTO, Guestbook> pageResultDTO
		= guestbookService.getList(pageRequestDTO);
		
		for(GuestbookDTO dto : pageResultDTO.getDtoList()) {
			System.out.println(dto);
		}
		
		System.out.println(pageResultDTO.isNext());
		System.out.println(pageResultDTO.getTotalPage());
		System.out.println(pageResultDTO.isPrev());
		
		System.out.println("=======================");
		
		pageResultDTO.getPageList().forEach(i -> {
			System.out.println(i);
		});
	}
}
