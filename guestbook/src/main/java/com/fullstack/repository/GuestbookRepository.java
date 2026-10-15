package com.fullstack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.fullstack.entity.Guestbook;

public interface GuestbookRepository extends JpaRepository<Guestbook, Long>, 
	QuerydslPredicateExecutor<Guestbook>{
	
}
