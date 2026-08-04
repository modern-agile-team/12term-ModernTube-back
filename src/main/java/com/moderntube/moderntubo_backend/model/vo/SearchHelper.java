package com.moderntube.moderntubo_backend.model.vo;

import com.moderntube.moderntubo_backend.util.AppConstants;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class SearchHelper {

    private Long boardId; // 유저 고유 번호 저장

    private String searchCode; // 검색 종류 (예: 일상: 1, 정보: 5, 질문: 7, 후기: 9)
    private String searchKeyword; // 검색 내용
    private String searchType; // 검색 타입 (예: 제목: tb.title, 내용: tb.contents, 사용자: u.username)
    private String searchState; // 검색 정렬 방식 (예: 전체: all, 인기글: best, 최신글: new, 내 글: my)
    private int size = Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE);
    private int page = Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER);

    @Builder
    public SearchHelper(Long boardId, String searchCode, String searchKeyword, String searchType, String searchState, int size, int page) {
        this.boardId = boardId;
        this.searchCode = searchCode;
        this.searchKeyword = searchKeyword;
        this.searchType = searchType;
        this.searchState = searchState;
        this.size = size;
        this.page = page;
    }

}
