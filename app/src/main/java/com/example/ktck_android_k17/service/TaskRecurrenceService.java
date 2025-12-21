package com.example.ktck_android_k17.service;

import android.util.Log;

import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.dao.TaskRecurrenceDAO;
import com.example.ktck_android_k17.model.Task;
import com.example.ktck_android_k17.model.TaskRecurrence;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Service để xử lý việc tạo task tự động dựa trên recurrence pattern.
 */
public class TaskRecurrenceService {

    private static final String TAG = "TaskRecurrenceService";
    private TaskDAO taskDAO;
    private TaskRecurrenceDAO recurrenceDAO;

    public TaskRecurrenceService() {
        taskDAO = new TaskDAO();
        recurrenceDAO = new TaskRecurrenceDAO();
    }

    /**
     * Kiểm tra và tạo các task mới dựa trên recurrence patterns.
     * Nên được gọi khi app khởi động hoặc định kỳ.
     * DEPRECATED: Sử dụng generateAllRecurringInstances() thay thế.
     */
    public void checkAndGenerateRecurringTasks() {
        Log.d(TAG, "Bắt đầu kiểm tra recurring tasks...");
        
        List<TaskRecurrence> activeRecurrences = recurrenceDAO.findActiveRecurrences();
        Log.d(TAG, "Tìm thấy " + activeRecurrences.size() + " recurring patterns đang active");

        for (TaskRecurrence recurrence : activeRecurrences) {
            try {
                checkAndGenerateTask(recurrence);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi xử lý recurrence cho task " + recurrence.getTaskId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Tạo tất cả task instances ngay lập tức từ startDate đến recurrenceEndDate.
     * 
     * @param recurrence TaskRecurrence pattern
     */
    public void generateAllRecurringInstances(TaskRecurrence recurrence) {
        Log.d(TAG, "Bắt đầu tạo tất cả instances cho recurrence " + recurrence.getId());
        
        // Lấy task gốc
        Task originalTask = taskDAO.findById(recurrence.getTaskId());
        if (originalTask == null) {
            Log.w(TAG, "Task gốc không tồn tại: " + recurrence.getTaskId());
            return;
        }

        // Xác định startDate và endDate
        String startDate = originalTask.getStartDate();
        if (startDate == null || startDate.isEmpty()) {
            // Nếu không có startDate, dùng dueDate hoặc ngày hiện tại
            startDate = originalTask.getDueDate();
            if (startDate == null || startDate.isEmpty()) {
                startDate = getTodayDateString();
            }
        }

        String endDate = recurrence.getRecurrenceEndDate();
        if (endDate == null || endDate.isEmpty()) {
            // Nếu không có recurrenceEndDate, set mặc định là startDate + 1 năm
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(startDate));
                cal.add(Calendar.YEAR, 1);
                endDate = sdf.format(cal.getTime());
                Log.d(TAG, "Không có recurrenceEndDate, set mặc định: " + endDate);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi tính toán endDate mặc định: " + e.getMessage());
                return;
            }
        }

        // Validate startDate < endDate
        if (startDate.compareTo(endDate) >= 0) {
            Log.e(TAG, "startDate (" + startDate + ") phải nhỏ hơn endDate (" + endDate + ")");
            return;
        }

        // Tính toán tất cả các ngày từ startDate đến endDate
        List<String> allDates = calculateAllOccurrenceDates(recurrence, startDate, endDate);
        Log.d(TAG, "Sẽ tạo " + allDates.size() + " task instances");

        // Tạo task instance cho mỗi ngày
        int createdCount = 0;
        for (String date : allDates) {
            try {
                createRecurringTaskInstance(originalTask, recurrence, date, endDate);
                createdCount++;
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi tạo task instance cho ngày " + date + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "Đã tạo " + createdCount + " task instances thành công");
    }

    /**
     * Kiểm tra và tạo task mới cho một recurrence pattern cụ thể.
     */
    private void checkAndGenerateTask(TaskRecurrence recurrence) {
        // Lấy task gốc
        Task originalTask = taskDAO.findById(recurrence.getTaskId());
        if (originalTask == null) {
            Log.w(TAG, "Task gốc không tồn tại: " + recurrence.getTaskId());
            recurrenceDAO.deactivate(recurrence.getId());
            return;
        }

        // Kiểm tra xem có cần tạo task mới không
        String nextDate = calculateNextOccurrenceDate(recurrence);
        if (nextDate == null) {
            Log.d(TAG, "Recurrence đã hết hạn hoặc đạt giới hạn: " + recurrence.getId());
            recurrenceDAO.deactivate(recurrence.getId());
            return;
        }

        // Kiểm tra xem đã đến ngày tạo task chưa
        String today = getTodayDateString();
        if (nextDate.compareTo(today) <= 0) {
            // Cần tạo task mới
            createRecurringTaskInstance(originalTask, recurrence, nextDate);
            
            // Cập nhật last_generated_date
            recurrence.setLastGeneratedDate(nextDate);
            recurrenceDAO.update(recurrence);
            
            Log.d(TAG, "Đã tạo task mới từ recurrence " + recurrence.getId() + " cho ngày " + nextDate);
        }
    }

    /**
     * Tính toán ngày tiếp theo cần tạo task dựa trên recurrence pattern.
     */
    private String calculateNextOccurrenceDate(TaskRecurrence recurrence) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Nếu có last_generated_date, bắt đầu từ đó, nếu không thì từ ngày tạo task gốc
        String startDate = recurrence.getLastGeneratedDate();
        if (startDate == null || startDate.isEmpty()) {
            Task originalTask = taskDAO.findById(recurrence.getTaskId());
            if (originalTask != null && originalTask.getDueDate() != null && !originalTask.getDueDate().isEmpty()) {
                startDate = originalTask.getDueDate();
            } else {
                startDate = getTodayDateString();
            }
        }

        try {
            Date start = sdf.parse(startDate);
            if (start == null) {
                return null;
            }
            cal.setTime(start);

            String type = recurrence.getRecurrenceType();
            int interval = recurrence.getRecurrenceInterval();

            switch (type) {
                case TaskRecurrence.TYPE_DAILY:
                    cal.add(Calendar.DAY_OF_MONTH, interval);
                    break;

                case TaskRecurrence.TYPE_WEEKLY:
                    // Nếu có recurrenceDays, chọn ngày tiếp theo trong tuần
                    if (recurrence.getRecurrenceDays() != null && !recurrence.getRecurrenceDays().isEmpty()) {
                        cal = calculateNextWeeklyDate(cal, recurrence.getRecurrenceDays(), interval);
                    } else {
                        cal.add(Calendar.WEEK_OF_YEAR, interval);
                    }
                    break;

                case TaskRecurrence.TYPE_MONTHLY:
                    if (recurrence.getRecurrenceDayOfMonth() != null) {
                        cal = calculateNextMonthlyDate(cal, recurrence.getRecurrenceDayOfMonth(), interval);
                    } else {
                        cal.add(Calendar.MONTH, interval);
                    }
                    break;

                case TaskRecurrence.TYPE_CUSTOM:
                    // Custom: interval có thể là days, weeks, hoặc months
                    // Giả sử là days cho đơn giản
                    cal.add(Calendar.DAY_OF_MONTH, interval);
                    break;

                default:
                    return null;
            }

            String nextDate = sdf.format(cal.getTime());

            // Kiểm tra end date hoặc count limit
            if (recurrence.getRecurrenceEndDate() != null && !recurrence.getRecurrenceEndDate().isEmpty()) {
                if (nextDate.compareTo(recurrence.getRecurrenceEndDate()) > 0) {
                    return null; // Đã vượt quá end date
                }
            }

            // TODO: Kiểm tra recurrence_count nếu cần

            return nextDate;

        } catch (Exception e) {
            Log.e(TAG, "Lỗi tính toán next occurrence date: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Tính toán ngày tiếp theo cho weekly recurrence với các ngày cụ thể.
     * recurrenceDays format: "1,3,5" (1=Monday, 7=Sunday in database format)
     */
    private Calendar calculateNextWeeklyDate(Calendar startCal, String recurrenceDays, int interval) {
        // recurrenceDays format: "1,3,5" (Mon,Wed,Fri) - database format: 1=Monday, 7=Sunday
        String[] days = recurrenceDays.split(",");
        int[] dayNumbers = new int[days.length];
        for (int i = 0; i < days.length; i++) {
            dayNumbers[i] = Integer.parseInt(days[i].trim());
        }

        Calendar cal = (Calendar) startCal.clone();
        // Convert Calendar day of week (1=Sunday, 2=Monday, ..., 7=Saturday) to database format (1=Monday, 7=Sunday)
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int currentDayDb = (currentDayOfWeek == Calendar.SUNDAY) ? 7 : currentDayOfWeek - 1;
        
        // Tìm ngày tiếp theo trong tuần hiện tại
        for (int dayDb : dayNumbers) {
            if (dayDb > currentDayDb) {
                // Calculate days to add
                int daysToAdd = dayDb - currentDayDb;
                cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
                return cal;
            }
        }

        // Nếu không tìm thấy trong tuần này, chuyển sang tuần tiếp theo
        cal.add(Calendar.WEEK_OF_YEAR, interval);
        // Set to first day in the list
        int firstDayDb = dayNumbers[0];
        // Convert database format to Calendar format
        int firstDayCalendar = (firstDayDb == 7) ? Calendar.SUNDAY : firstDayDb + 1;
        // Get current day of week after adding interval weeks
        int currentDayOfWeekAfterInterval = cal.get(Calendar.DAY_OF_WEEK);
        int currentDayDbAfterInterval = (currentDayOfWeekAfterInterval == Calendar.SUNDAY) ? 7 : currentDayOfWeekAfterInterval - 1;
        // Calculate days to add to reach first day
        int daysToAdd = firstDayDb - currentDayDbAfterInterval;
        if (daysToAdd < 0) daysToAdd += 7; // Wrap around to next week
        cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
        return cal;
    }

    /**
     * Tính toán ngày tiếp theo cho monthly recurrence với ngày cụ thể trong tháng.
     */
    private Calendar calculateNextMonthlyDate(Calendar startCal, int dayOfMonth, int interval) {
        Calendar cal = (Calendar) startCal.clone();
        cal.add(Calendar.MONTH, interval);
        
        // Đảm bảo ngày hợp lệ (xử lý tháng có ít ngày hơn)
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (dayOfMonth > maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, maxDay);
        } else {
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        }
        
        return cal;
    }

    /**
     * Tính toán tất cả các ngày occurrence từ startDate đến endDate.
     */
    private List<String> calculateAllOccurrenceDates(TaskRecurrence recurrence, String startDate, String endDate) {
        List<String> dates = new java.util.ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            String type = recurrence.getRecurrenceType();
            int interval = recurrence.getRecurrenceInterval();

            while (true) {
                String currentDate = sdf.format(cal.getTime());
                
                // Kiểm tra đã vượt quá endDate chưa
                if (currentDate.compareTo(endDate) > 0) {
                    break;
                }

                // Thêm ngày vào danh sách
                dates.add(currentDate);

                // Tính ngày tiếp theo dựa trên recurrence type
                switch (type) {
                    case TaskRecurrence.TYPE_DAILY:
                        cal.add(Calendar.DAY_OF_MONTH, interval);
                        break;

                    case TaskRecurrence.TYPE_WEEKLY:
                        if (recurrence.getRecurrenceDays() != null && !recurrence.getRecurrenceDays().isEmpty()) {
                            cal = calculateNextWeeklyDate(cal, recurrence.getRecurrenceDays(), interval);
                        } else {
                            cal.add(Calendar.WEEK_OF_YEAR, interval);
                        }
                        break;

                    case TaskRecurrence.TYPE_MONTHLY:
                        if (recurrence.getRecurrenceDayOfMonth() != null) {
                            cal = calculateNextMonthlyDate(cal, recurrence.getRecurrenceDayOfMonth(), interval);
                        } else {
                            cal.add(Calendar.MONTH, interval);
                        }
                        break;

                    case TaskRecurrence.TYPE_CUSTOM:
                        cal.add(Calendar.DAY_OF_MONTH, interval);
                        break;

                    default:
                        return dates; // Unknown type, return what we have
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tính toán all occurrence dates: " + e.getMessage(), e);
        }

        return dates;
    }

    /**
     * Tạo một instance mới của task dựa trên task gốc và recurrence.
     */
    private void createRecurringTaskInstance(Task originalTask, TaskRecurrence recurrence, String startDate, String endDate) {
        // Validate startDate < endDate
        if (startDate.compareTo(endDate) >= 0) {
            Log.w(TAG, "Bỏ qua instance: startDate (" + startDate + ") >= endDate (" + endDate + ")");
            return;
        }

        Task newTask = new Task();
        newTask.setUserId(originalTask.getUserId());
        newTask.setCategoryId(originalTask.getCategoryId());
        newTask.setTitle(originalTask.getTitle());
        newTask.setDescription(originalTask.getDescription());
        newTask.setStatus(Task.STATUS_PENDING); // Task mới luôn là pending
        newTask.setPriority(originalTask.getPriority());
        newTask.setDueDate(startDate); // dueDate = startDate của instance này
        newTask.setStartDate(startDate);
        newTask.setEndDate(endDate); // endDate = recurrenceEndDate

        // Insert task mới
        int newTaskId = taskDAO.insertAndGetId(newTask);
        if (newTaskId > 0) {
            Log.d(TAG, "Đã tạo task mới với ID: " + newTaskId + ", startDate: " + startDate + ", endDate: " + endDate);
        } else {
            Log.e(TAG, "Không thể tạo task mới từ recurrence");
        }
    }

    /**
     * Tạo một instance mới của task dựa trên task gốc và recurrence (backward compatibility).
     */
    private void createRecurringTaskInstance(Task originalTask, TaskRecurrence recurrence, String dueDate) {
        // For backward compatibility, use dueDate as startDate and recurrenceEndDate as endDate
        String endDate = recurrence.getRecurrenceEndDate();
        if (endDate == null || endDate.isEmpty()) {
            // Set default endDate = startDate + 1 day
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(dueDate));
                cal.add(Calendar.DAY_OF_MONTH, 1);
                endDate = sdf.format(cal.getTime());
            } catch (Exception e) {
                endDate = dueDate; // Fallback
            }
        }
        createRecurringTaskInstance(originalTask, recurrence, dueDate, endDate);
    }

    /**
     * Lấy ngày hôm nay dưới dạng string (yyyy-MM-dd).
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}

