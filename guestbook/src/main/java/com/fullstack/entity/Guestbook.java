package com.fullstack.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Guestbook extends BaseEntity {
	// 컬럼은 등록일, 수정일(이건 BaseEntity에서 처리)
	// 작성자, 내용, 제목, 으로 구성
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long gno;
	
	@Column(length = 100, nullable = false)
	private String title;
	
	@Column(length = 1500, nullable = false)
	private String content;
	
	@Column(length = 50, nullable = false)
	private String writer;
	
	public void changeTitle(String title) {
		this.title = title;
	}
	public void changeContent(String content) {
		this.content = content;
	}
}
