package cn.rabitown.rabisystem.modules.spirit.utils;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import java.util.*;

/**
 * 节日判定与名称工具类 - 编程猫猫版
 */
public class HolidayUtil {

    // 公历固定节日映射 (MM-DD -> 节日名)
    private static final Map<String, String> SOLAR_HOLIDAYS = new HashMap<>();
    static {
        SOLAR_HOLIDAYS.put("01-01", "元旦节");
        SOLAR_HOLIDAYS.put("02-14", "情人节");
        SOLAR_HOLIDAYS.put("03-08", "妇女节");
        SOLAR_HOLIDAYS.put("03-12", "植树节");
        SOLAR_HOLIDAYS.put("05-01", "劳动节");
        SOLAR_HOLIDAYS.put("06-01", "儿童节");
        SOLAR_HOLIDAYS.put("10-01", "国庆节");
        SOLAR_HOLIDAYS.put("12-24", "平安夜");
        SOLAR_HOLIDAYS.put("12-25", "圣诞节");
    }

    /**
     * 获取指定日期的节日名称
     * @return 如果是节日则返回名称（如"春节"），否则返回 null
     */
    public static String getHolidayName(Calendar cal) {
        Solar solar = Solar.fromCalendar(cal);
        Lunar lunar = solar.getLunar();

        // 1. 检查公历固定节日
        String solarKey = String.format("%02d-%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        if (SOLAR_HOLIDAYS.containsKey(solarKey)) {
            return SOLAR_HOLIDAYS.get(solarKey);
        }

        // 2. 检查农历节日
        int lMonth = lunar.getMonth();
        int lDay = lunar.getDay();

        if (lMonth == 1 && lDay == 1) return "春节";
        if (lMonth == 1 && lDay == 15) return "元宵节";
        if (lMonth == 5 && lDay == 5) return "端午节";
        if (lMonth == 7 && lDay == 7) return "七夕节";
        if (lMonth == 8 && lDay == 15) return "中秋节";
        if (lMonth == 9 && lDay == 9) return "重阳节";
        if (lMonth == 12 && lDay == 8) return "腊八节";
        if (lMonth == 12 && lDay == 23) return "小年";

        // 检查除夕 (看明天是不是正月初一)
        Calendar nextDay = (Calendar) cal.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        Lunar nextLunar = Solar.fromCalendar(nextDay).getLunar();
        if (nextLunar.getMonth() == 1 && nextLunar.getDay() == 1) return "除夕";

        // 3. 检查二十四节气
        String jieQi = lunar.getJieQi();
        if ("清明".equals(jieQi)) return "清明节";
        if ("冬至".equals(jieQi)) return "冬至节";

        return null; // 不是节日
    }

    public static boolean isHoliday(Calendar cal) {
        return getHolidayName(cal) != null;
    }

    public static String getFullDateKey(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }
}