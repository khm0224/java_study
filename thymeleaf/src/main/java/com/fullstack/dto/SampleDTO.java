package com.fullstack.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true) // 객체 복제해서 넘겨주라는 의미
public class SampleDTO {
	private long sno;
	private String first;
	private String last;
	private LocalDateTime regTime;
}
