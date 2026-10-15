package com.fullstack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fullstack.entity.MovieImage;

public interface MovieimageRepository extends JpaRepository<MovieImage, Long> {

}
