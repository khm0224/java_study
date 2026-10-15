package com.fullstack.entity;
/*
 * bno = 기본키 / 제목, 내용 등을 컬럼으로 설정
 */

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString(exclude = "writer") // toString 할 때 writer는 제외
public class Board extends BaseEntity{ // 만들어 둔 BaseEntity를 상속 받아야 BaseEntity껄 사용 가능
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bno;
	
	private String title;
	private String content;
	
	// 참조 선언
	@ManyToOne(fetch = FetchType.LAZY)
	// fetch = FetchType.LAZY 이게 있으면 join이 기본적으로 사용 안하게 가능
	// 대신 join 쓸거면 본인이 직접 join를 날려야함
	private Member writer;
	
	public void changeTitle(String title) {
		this.title = title;
	} public void changeContent(String content) {
		this.content = content;
	}
}
