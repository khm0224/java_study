package com.fullstack;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fullstack.dto.SampleDTO;

import lombok.extern.log4j.Log4j2;

@Controller
@RequestMapping("/sample") // Context 명
@Log4j2
public class SampleController {
	
	@GetMapping("/ex1")
	public void ex1() {
		log.info("ex1...요청됨");
	}
	
	@GetMapping({"/ex2", "/exLink"})
	// model 객체에 DTO 객체를 전달하도록 한다
	public void exModel(Model model) {
		// 모델 객체는 Viewer 에 특저 영역에 해당하는 Data 를 전송하는 역할
		List<SampleDTO> list = IntStream.rangeClosed(1, 20)
				.asLongStream().mapToObj(i -> {
					SampleDTO dto = SampleDTO.builder().sno(i).first("first.."+i)
							.last("last.."+i).regTime(LocalDateTime.now()).build();
					return dto;
				}).collect(Collectors.toList());
		model.addAttribute("list", list);
	}
	
	@GetMapping("/exInline")
	public String exInline(RedirectAttributes attributes) {
		log.info("exInline 호출됨");
		SampleDTO dto = SampleDTO.builder().sno(100L).first("first.."+100)
				.last("last.."+100).regTime(LocalDateTime.now()).build();
		
		attributes.addFlashAttribute("result", "success");
		attributes.addFlashAttribute("dto", dto);
		return "redirect:/sample/ex3";
		// 리다이엑트는 1회 요청이라 f5 눌리면 없어짐
	}
	
	@GetMapping("ex3")
	public void ex3() {
		log.info("ex3 호출됨");
	}
}
