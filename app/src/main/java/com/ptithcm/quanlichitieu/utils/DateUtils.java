package com.ptithcm.quanlichitieu.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * DateUtils - Utility class để xử lý các thao tác liên quan đến ngày tháng.
 * 
 * Chức năng chính:
 * - Format ngày giờ theo định dạng Việt Nam
 * - Format thứ trong tuần bằng tiếng Việt
 * - Tính toán khoảng thời gian theo tháng
 * - Nhóm giao dịch theo ngày
 * 
 * Tuân thủ Single Responsibility Principle - chỉ xử lý logic ngày tháng.
 */
public class DateUtils {

    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");

    /**
     * Lấy số ngày trong tháng theo offset.
     *
     * @param monthOffset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     */
    public static int getDaysInMonth(int monthOffset) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.add(Calendar.MONTH, monthOffset);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Lấy timestamp cuối ngày hôm nay (23:59:59.999).
     */
    public static long getEndOfTodayTimestamp() {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * Format ngày theo định dạng dd/MM/yyyy.
     * Ví dụ: 18/3/2026
     * 
     * @param timestamp Timestamp (milliseconds since epoch)
     * @return String ngày đã format
     */
    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", VIETNAM_LOCALE);
        return sdf.format(timestamp);
    }

    /**
     * Format thứ trong tuần bằng tiếng Việt.
     * 
     * Mapping:
     * - Chủ nhật (Calendar.SUNDAY = 1) → "Chủ nhật"
     * - Thứ 2 (Calendar.MONDAY = 2) → "Thứ 2"
     * - Thứ 3 (Calendar.TUESDAY = 3) → "Thứ 3"
     * - ...
     * - Thứ 7 (Calendar.SATURDAY = 7) → "Thứ 7"
     * 
     * @param timestamp Timestamp (milliseconds since epoch)
     * @return String thứ trong tuần bằng tiếng Việt
     */
    public static String formatDayOfWeek(long timestamp) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.setTimeInMillis(timestamp);
        
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "Chủ nhật";
            case Calendar.MONDAY:
                return "Thứ 2";
            case Calendar.TUESDAY:
                return "Thứ 3";
            case Calendar.WEDNESDAY:
                return "Thứ 4";
            case Calendar.THURSDAY:
                return "Thứ 5";
            case Calendar.FRIDAY:
                return "Thứ 6";
            case Calendar.SATURDAY:
                return "Thứ 7";
            default:
                return "";
        }
    }

    /**
     * Lấy timestamp của đầu tháng (00:00:00 ngày 1).
     * 
     * @param monthOffset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     * @return Timestamp của đầu tháng
     */
    public static long getMonthStartTimestamp(int monthOffset) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        
        // Thêm offset tháng
        calendar.add(Calendar.MONTH, monthOffset);
        
        // Set về ngày 1 của tháng
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        
        // Set về đầu ngày (00:00:00.000)
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar.getTimeInMillis();
    }

    /**
     * Lấy timestamp của cuối tháng (23:59:59.999 ngày cuối cùng).
     * 
     * @param monthOffset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     * @return Timestamp của cuối tháng
     */
    public static long getMonthEndTimestamp(int monthOffset) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        
        // Thêm offset tháng
        calendar.add(Calendar.MONTH, monthOffset);
        
        // Set về ngày cuối cùng của tháng
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        
        // Set về cuối ngày (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        
        return calendar.getTimeInMillis();
    }

    /**
     * Lấy timestamp của ngày đầu tuần (00:00:00 Thứ 2).
     *
     * @param weekOffset Offset tuần (-1 = tuần trước, 0 = tuần này, 1 = tuần sau)
     * @return Timestamp của đầu tuần
     */
    public static long getWeekStartTimestamp(int weekOffset) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Tinh khoang cach ve Thu 2
        int offset = (dayOfWeek == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dayOfWeek;
        calendar.add(Calendar.DAY_OF_MONTH, offset);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Lấy timestamp của cuối tuần (23:59:59.999 Chủ nhật).
     *
     * @param weekOffset Offset tuần (-1 = tuần trước, 0 = tuần này, 1 = tuần sau)
     * @return Timestamp của cuối tuần
     */
    public static long getWeekEndTimestamp(int weekOffset) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffset);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Tinh khoang cach den Chu Nhat
        int offset = (dayOfWeek == Calendar.SUNDAY) ? 0 : 8 - dayOfWeek;
        calendar.add(Calendar.DAY_OF_MONTH, offset);

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * Lấy key ngày để nhóm giao dịch (format: yyyyMMdd).
     * Dùng để group các giao dịch cùng ngày.
     * 
     * @param timestamp Timestamp (milliseconds since epoch)
     * @return String key ngày (ví dụ: "20260318")
     */
    public static String getDateKey(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", VIETNAM_LOCALE);
        return sdf.format(timestamp);
    }

    /**
     * Kiểm tra xem timestamp có thuộc ngày hôm nay không.
     * 
     * @param timestamp Timestamp cần kiểm tra
     * @return true nếu là hôm nay, false nếu không
     */
    public static boolean isToday(long timestamp) {
        Calendar cal1 = Calendar.getInstance(VIETNAM_LOCALE);
        Calendar cal2 = Calendar.getInstance(VIETNAM_LOCALE);
        cal2.setTimeInMillis(timestamp);
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Lấy tên tháng hiện tại theo offset.
     * 
     * @param monthOffset Offset tháng (-1 = tháng trước, 0 = tháng này, 1 = tháng sau)
     * @return Tên tháng (ví dụ: "Tháng 3")
     */
    public static String getMonthName(int monthOffset) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.add(Calendar.MONTH, monthOffset);
        
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH là 0-based
        return "Tháng " + month;
    }

    /**
     * Normalize timestamp về đầu ngày (00:00:00).
     * Dùng để so sánh ngày mà không quan tâm giờ phút giây.
     * 
     * @param timestamp Timestamp gốc
     * @return Timestamp đã normalize về đầu ngày
     */
    public static long normalizeToDayStart(long timestamp) {
        Calendar calendar = Calendar.getInstance(VIETNAM_LOCALE);
        calendar.setTimeInMillis(timestamp);
        
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        return calendar.getTimeInMillis();
    }
}
