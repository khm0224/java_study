package com.fullstack.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDTO {
	private Long rervienum;
	private Long mno;
	
	private Long mid; // member id
	private String nickname;
	private String email;
	private int grade;
	private String text;
	private LocalDateTime regDate, modDate;
}
