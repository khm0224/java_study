package com.fullstack.service;

import java.util.List;

import com.fullstack.dto.ReviewDTO;
import com.fullstack.entity.Member;
import com.fullstack.entity.Movie;
import com.fullstack.entity.Review;

public interface ReviewService {
	// 영화의 모든 리뷰 리턴
	List<ReviewDTO> getListOfMovie(Long mno);

	// 영화 리뷰 추가
	Long register(ReviewDTO reviewDTO);

	// 특정 영화 리뷰 수정
	void modify(ReviewDTO reviewDTO);

	// 영화 리뷰 삭제
	void remove(Long reviewnum);

	default Review dtoToEntity(ReviewDTO movieReviewDTO) {

		Review movieReview = Review.builder().reviewnum(movieReviewDTO.getRervienum())
				.movie(Movie.builder().mno(movieReviewDTO.getMno()).build())
				.member(Member.builder().mid(movieReviewDTO.getMid()).build()).grade(movieReviewDTO.getGrade())
				.text(movieReviewDTO.getText()).build();

		return movieReview;
	}

	default ReviewDTO entityToDto(Review movieReview) {

		ReviewDTO movieReviewDTO = ReviewDTO.builder().rervienum(movieReview.getReviewnum())
				.mno(movieReview.getMovie().getMno()).mid(movieReview.getMember().getMid())
				.nickname(movieReview.getMember().getNickname()).email(movieReview.getMember().getEmail())
				.grade(movieReview.getGrade()).text(movieReview.getText()).regDate(movieReview.getRegDate())
				.modDate(movieReview.getModDate()).build();

		return movieReviewDTO;
	}
}
