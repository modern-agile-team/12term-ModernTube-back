/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.moderntube.moderntubo_backend.util;

import com.moderntube.moderntubo_backend.model.vo.SearchHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Util {

    private Util() {
        throw new UnsupportedOperationException("Cannot instantiate a Util class");
    }

    /**
     * uuid
     * @return
     */
    public static String generateRandomUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 랜덤 문자열
     * @param size
     * @return
     */
    public static String randomString(int size) {
        if (size > 0) {
            char[] tmp = new char[size];
            for (int i = 0; i < tmp.length; i++) {
                int div = (int) Math.floor(Math.random() * 2);
                if (div == 0) {
                    tmp[i] = (char) (Math.random() * 10 + '0');
                } else {
                    tmp[i] = (char) (Math.random() * 26 + 'A');
                }
            }
            return new String(tmp);
        }
        return null;
    }

    /**
     * 지금 date time을 string 로
     * @return
     */
    public static String dateToString() {
        SimpleDateFormat nowDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA);
        String format = nowDate.format(System.currentTimeMillis());
        return format;
    }


    /**
     * 날짜를 int로 변환
     * @param date
     * @return
     */
    public static int getDateByInteger(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        return Integer.parseInt(simpleDateFormat.format(date));
    }

    /**
     * 날짜를 string로 변환
     * @param date
     * @return
     */
    public static String getDateByString(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        return simpleDateFormat.format(date);
    }

    /**
     * String -> Date
     * @param date
     * @return
     * @throws ParseException
     */
    public static Date getStringToDate(String date) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Date returnDate = simpleDateFormat.parse(date);
        return returnDate;
    }

    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if ((o instanceof String) && (((String)o).trim().length()) == 0) {
            return true;
        }
        if (o instanceof Map) {
            return ((Map<?, ?>)o).isEmpty();
        }
        if (o instanceof List) {
            return ((List<?>)o).isEmpty();
        }
        if (o instanceof Object[]) {
            return (((Object[])o).length == 0);
        }
        return false;
    }

    /**
     * searchHelper로 검색한 내용을 hashMap으로 반환
     * @param searchHelper
     * @param elementList
     * @param totalElements
     * @return
     */
    public static HashMap<String, Object> makeResultMap(SearchHelper searchHelper, List<?> elementList, float totalElements) {
        HashMap<String, Object> resultMap = new HashMap<>();

        resultMap.put("list", elementList);
        resultMap.put("totalElements", totalElements);
        resultMap.put("size", searchHelper.getSize());
        resultMap.put("page", searchHelper.getPage());
        resultMap.put("totalPages", Math.ceil(totalElements / searchHelper.getSize()));
        resultMap.put("last", searchHelper.getPage() >= searchHelper.getSize());

        return resultMap;
    }


}
