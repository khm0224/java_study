package com.fullstack.dto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovieImageDTO {
	private String uuid;
	private String imgName;
	private String path;
	
	// UI 쪽에서 사용할 getter 생성
	public String getImageURL() {
		try {
			return URLEncoder.encode(path + "/" + uuid+ "_" + imgName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// 썸네일 
	public String getThumbnailURL() {
		try {
			System.out.println("가능?");
			return URLEncoder.encode(path + "/s_" + uuid+ "_" + imgName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
