package com.ame.mihoyosign.util;

import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class Utils {

    private static final Random RANDOM = new Random();

    private static final Character[] CHARACTERS = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    private Utils() {
    }

    public static Map<String, String> getCookieByStr(String str) {
        String[] cookieStr = str.split(";");
        Map<String, String> cookie = new HashMap<>(cookieStr.length);
        for (String s : cookieStr) {
            String[] oneCookie = s.split("=");
            cookie.put(oneCookie[0], oneCookie[1]);
        }
        return cookie;
    }

    public static String getChinaTime(String format) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        java.util.Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    /**
     * 随机数
     */
    public static String getRandomFromArray(Character[] array, int count) {
        if (array == null) {
            array = CHARACTERS;
        }
        StringBuilder re = new StringBuilder();
        List<Character> list = Arrays.asList(array);
        List<Character> arrList = new ArrayList<>(list);
        for (int i = 0; i < count; i++) {
            int t = RANDOM.nextInt(arrList.size());
            re.append(arrList.get(t));
            arrList.remove(t);
        }
        return re.toString();
    }

    public static long delay(String rangeStr) {
        if (rangeStr == null) {
            return -1;
        }
        List<Integer> range = getRange(rangeStr);
        int i = RANDOM.nextInt(range.get(1) - range.get(0)) + range.get(0);
        try {
            Thread.sleep(i * 1000L);
            return i;
        } catch (InterruptedException e) {
            log.warn("延迟执行出错");
            return -1;
        }
    }

    public static List<Integer> getRange(String rangeStr) {
        return JSONArray.parseArray(rangeStr, Integer.class);
    }

    public static String parseRange(String rangeStr, String rangeMax) throws Exception {
        List<Integer> maxRange = getRange(rangeMax);
        List<String> strings = Arrays.asList(rangeStr.split("~"));
        try {
            int a = Integer.parseInt(strings.get(0));
            int b = Integer.parseInt(strings.get(1));
            int[] ints = {a, b};
            Arrays.sort(ints);
            a = ints[0];
            b = ints[1];
            if (a == b) {
                b++;
            }
            if (maxRange != null && (a < maxRange.get(0) || b > maxRange.get(1))) {
                throw new Exception("超出范围,请位于" + maxRange + "之间");
            }
            return "[" + a + "," + b + "]";
        } catch (NumberFormatException e) {
            throw new Exception("格式错误,例 0~10");
        }
    }
}
