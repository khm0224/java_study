package com.fullstack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity // 엔티티 선언하면 이 클래스는 DB의 테이블이 됨(테이블이 없으면 생성을 함)
@Table(name = "tbl_memo") // 실제 DB에 저장되는 테이블 명
@ToString
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Memo {
	// 모든 entity 에는 반드시 기본키가 존재 해야한다
	@Id // 기본키 선언 어노테이션
	@GeneratedValue(strategy = GenerationType.IDENTITY) // AutoIncrement 부여
	private Long mno; // mno 는 기본키이고 AutoIncrement가 부여된 상태
	
	// 일반 컬럼 정의
	@Column(length = 200, nullable = false) // 해당 어노테이션은 컬럼의 속성, 길이 등을 선언 가능
	private String memoText;
}

/*
 * create table tbl_memo (
        mno bigint not null auto_increment,
        memo_text varchar(200) not null,
        primary key (mno)
    )
 * 위에 코드랑 이거랑 동일한거임
 * */