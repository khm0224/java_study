package com.fullstack.entity;

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
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"movie", "member"})
@Getter
public class Review extends BaseEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reviewnum;
	
	@ManyToOne(fetch = FetchType.LAZY)
	private Movie movie;
	@ManyToOne(fetch = FetchType.LAZY)
	private Member member;
	
	private int grade; // 평점
	private String text; // 리뷰내용
	
	public void changGrade(int grade) {
		this.grade = grade;
	}
	public void changeText(String text) {
		this.text = text;
	}
}
