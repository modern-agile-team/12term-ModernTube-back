package com.moderntube.moderntubo_backend.util;

import com.moderntube.moderntubo_backend.exception.BadRequestException;

public class ValidatePageNumberAndSize {

    public static void validatePageNumberAndSize(int page, int size) {
        if(page < 0) {
            throw new BadRequestException("페이지 번호는 0보다 작을 수 없습니다.");
        }

        if(size > AppConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException("페이지 사이즈는 " + AppConstants.MAX_PAGE_SIZE + "보다 클 수 없습니다.");
        }
    }

}
