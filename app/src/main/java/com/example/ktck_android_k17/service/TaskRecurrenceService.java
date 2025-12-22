package com.example.ktck_android_k17.service;

import android.util.Log;

import com.example.ktck_android_k17.adapter.TaskAdapter;
import com.example.ktck_android_k17.dao.TaskDAO;
import com.example.ktck_android_k17.dao.TaskExceptionDAO;
import com.example.ktck_android_k17.dao.TaskRecurrenceDAO;
import com.example.ktck_android_k17.dto.TaskDTO;
import com.example.ktck_android_k17.model.Task;
import com.example.ktck_android_k17.model.TaskException;
import com.example.ktck_android_k17.model.TaskRecurrence;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service để xử lý việc tạo task tự động dựa trên recurrence pattern.
 */
public class TaskRecurrenceService {

    private static final String TAG = "TaskRecurrenceService";
    private TaskDAO taskDAO;
    private TaskRecurrenceDAO recurrenceDAO;
    private TaskExceptionDAO exceptionDAO;

    public TaskRecurrenceService() {
        taskDAO = new TaskDAO();
        recurrenceDAO = new TaskRecurrenceDAO();
        exceptionDAO = new TaskExceptionDAO();
    }

    /**
     * Kiểm tra và tạo các task mới dựa trên recurrence patterns.
     * DEPRECATED: Không còn sử dụng vì instances được generate động khi hiển thị.
     */
    public void checkAndGenerateRecurringTasks() {
        // No longer needed - instances are generated dynamically in MainActivity
        Log.d(TAG, "checkAndGenerateRecurringTasks() is deprecated - instances are generated dynamically");
    }

    /**
     * Generate instances động trong date range cho một master task.
     * Không tạo instances trong database, chỉ tính toán và trả về TaskDTOs.
     * Kiểm tra exceptions trước khi generate.
     *
     * @param masterTask Master task với recurrence rule
     * @param startDate  Ngày bắt đầu (yyyy-MM-dd)
     * @param endDate    Ngày kết thúc (yyyy-MM-dd)
     * @return Danh sách TaskDTO instances
     */
    public List<TaskDTO> generateInstancesInRange(Task masterTask, String startDate, String endDate) {
        Log.d(TAG, "Generate instances cho master task " + masterTask.getId() + " từ " + startDate + " đến " + endDate);
        
        List<TaskDTO> instances = new ArrayList<>();
        
        // Lấy recurrence rule
        TaskRecurrence recurrence = recurrenceDAO.findByTaskId(masterTask.getId());
        if (recurrence == null || !recurrence.isActive()) {
            Log.w(TAG, "Không tìm thấy recurrence rule hoặc đã inactive cho task " + masterTask.getId());
            return instances;
        }

        // Lấy tất cả exceptions của master task
        List<TaskException> exceptions = exceptionDAO.findByMasterTaskId(masterTask.getId());
        Map<String, TaskException> exceptionMap = new HashMap<>();
        for (TaskException ex : exceptions) {
            exceptionMap.put(ex.getOriginalOccurrenceDate(), ex);
        }

        // Tính toán các occurrence dates
        List<String> occurrenceDates = calculateOccurrences(recurrence, startDate, endDate);
        
        // Tạo TaskDTO cho mỗi occurrence date
        for (String occurrenceDate : occurrenceDates) {
            // Kiểm tra xem có exception không
            TaskException exception = exceptionMap.get(occurrenceDate);
            
            if (exception != null) {
                // Có exception
                if (TaskException.TYPE_DELETED.equals(exception.getExceptionType())) {
                    // Instance bị xóa, bỏ qua
                    continue;
                } else if (TaskException.TYPE_MODIFIED.equals(exception.getExceptionType()) && exception.getModifiedTaskId() != null) {
                    // Instance bị chỉnh sửa, load task đã chỉnh sửa
                    Task modifiedTask = taskDAO.findById(exception.getModifiedTaskId());
                    if (modifiedTask != null) {
                        TaskDTO dto = TaskAdapter.toDTO(modifiedTask);
                        dto.setParentTaskId(masterTask.getId());
                        dto.setOccurrenceDate(occurrenceDate);
                        instances.add(dto);
                    }
                }
            } else {
                // Không có exception, tạo instance từ master task
                TaskDTO instanceDTO = createInstanceDTO(masterTask, occurrenceDate, recurrence);
                instances.add(instanceDTO);
            }
        }

        Log.d(TAG, "Đã generate " + instances.size() + " instances");
        return instances;
    }

    /**
     * Tạo TaskDTO cho một instance từ master task.
     */
    private TaskDTO createInstanceDTO(Task masterTask, String occurrenceDate, TaskRecurrence recurrence) {
        TaskDTO dto = TaskAdapter.toDTO(masterTask);
        dto.setId(0); // Virtual instance, không có ID trong database
        dto.setParentTaskId(masterTask.getId());
        dto.setIsRecurringMaster(false);
        dto.setOccurrenceDate(occurrenceDate);
        dto.setDueDate(occurrenceDate); // dueDate = occurrenceDate cho instance
        
        // Set recurrence info
        String recurrenceInfo = formatRecurrenceInfo(recurrence);
        dto.setRecurrenceInfo(recurrenceInfo);
        
        return dto;
    }

    /**
     * Format recurrence info thành chuỗi dễ đọc (VD: "Mỗi thứ 2, 4, 6").
     */
    private String formatRecurrenceInfo(TaskRecurrence recurrence) {
        String type = recurrence.getRecurrenceType();
        int interval = recurrence.getRecurrenceInterval();
        
        switch (type) {
            case TaskRecurrence.TYPE_DAILY:
                if (interval == 1) {
                    return "Hàng ngày";
                } else {
                    return "Mỗi " + interval + " ngày";
                }
            case TaskRecurrence.TYPE_WEEKLY:
                if (recurrence.getRecurrenceDays() != null && !recurrence.getRecurrenceDays().isEmpty()) {
                    String[] days = recurrence.getRecurrenceDays().split(",");
                    String[] dayNames = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < days.length; i++) {
                        if (i > 0) sb.append(", ");
                        int dayNum = Integer.parseInt(days[i].trim());
                        if (dayNum >= 1 && dayNum <= 7) {
                            sb.append(dayNames[dayNum - 1]);
                        }
                    }
                    return "Mỗi " + sb.toString();
                } else {
                    return interval == 1 ? "Hàng tuần" : "Mỗi " + interval + " tuần";
                }
            case TaskRecurrence.TYPE_MONTHLY:
                if (recurrence.getRecurrenceDayOfMonth() != null) {
                    return "Ngày " + recurrence.getRecurrenceDayOfMonth() + " hàng tháng";
                } else {
                    return interval == 1 ? "Hàng tháng" : "Mỗi " + interval + " tháng";
                }
            case TaskRecurrence.TYPE_CUSTOM:
                return "Tùy chỉnh";
            default:
                return "Lặp lại";
        }
    }

    /**
     * Kiểm tra và tạo task mới cho một recurrence pattern cụ thể.
     * DEPRECATED: Không còn sử dụng vì instances được generate động.
     */
    private void checkAndGenerateTask(TaskRecurrence recurrence) {
        // No longer needed - instances are generated dynamically
        Log.d(TAG, "checkAndGenerateTask() is deprecated");
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
     * Tính toán các ngày occurrence từ startDate đến endDate dựa trên recurrence rule.
     * 
     * @param recurrence Recurrence rule
     * @param startDate  Ngày bắt đầu (yyyy-MM-dd)
     * @param endDate    Ngày kết thúc (yyyy-MM-dd)
     * @return Danh sách các ngày occurrence
     */
    public List<String> calculateOccurrences(TaskRecurrence recurrence, String startDate, String endDate) {
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
     * Tạo exception khi user chỉnh sửa riêng một instance.
     *
     * @param masterTaskId      ID của master task
     * @param occurrenceDate    Ngày occurrence bị chỉnh sửa (yyyy-MM-dd)
     * @param modifiedTask      Task đã được chỉnh sửa (nếu null thì exception type là 'deleted')
     * @return true nếu thành công
     */
    public boolean createException(int masterTaskId, String occurrenceDate, Task modifiedTask) {
        Log.d(TAG, "Tạo exception cho master task " + masterTaskId + ", occurrence: " + occurrenceDate);
        
        String exceptionType;
        Integer modifiedTaskId = null;
        
        if (modifiedTask != null) {
            exceptionType = TaskException.TYPE_MODIFIED;
            // Lưu modified task vào database
            modifiedTask.setParentTaskId(masterTaskId);
            modifiedTask.setIsMaster(false);
            modifiedTask.setOccurrenceDate(occurrenceDate);
            modifiedTaskId = taskDAO.insertAndGetId(modifiedTask);
            if (modifiedTaskId <= 0) {
                Log.e(TAG, "Không thể tạo modified task");
                return false;
            }
        } else {
            exceptionType = TaskException.TYPE_DELETED;
        }
        
        // Tạo exception record
        TaskException exception = new TaskException(masterTaskId, occurrenceDate, exceptionType);
        exception.setModifiedTaskId(modifiedTaskId);
        
        return exceptionDAO.insert(exception);
    }

    /**
     * Split recurrence khi user chọn "Từ lần này trở đi".
     * Cắt recurrence cũ trước splitDate và tạo recurrence mới từ splitDate.
     *
     * @param masterTaskId ID của master task
     * @param splitDate     Ngày split (yyyy-MM-dd)
     * @return true nếu thành công
     */
    public boolean splitRecurrence(int masterTaskId, String splitDate) {
        Log.d(TAG, "Split recurrence cho master task " + masterTaskId + " tại ngày " + splitDate);
        
        // Lấy recurrence hiện tại
        TaskRecurrence oldRecurrence = recurrenceDAO.findByTaskId(masterTaskId);
        if (oldRecurrence == null) {
            Log.e(TAG, "Không tìm thấy recurrence cho task " + masterTaskId);
            return false;
        }
        
        // Cập nhật recurrence cũ: set endDate = splitDate - 1 day
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(splitDate));
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String oldEndDate = sdf.format(cal.getTime());
            
            oldRecurrence.setRecurrenceEndDate(oldEndDate);
            recurrenceDAO.update(oldRecurrence);
            
            // Tạo master task mới từ splitDate
            Task oldMasterTask = taskDAO.findById(masterTaskId);
            if (oldMasterTask == null) {
                Log.e(TAG, "Không tìm thấy master task " + masterTaskId);
                return false;
            }
            
            Task newMasterTask = new Task();
            newMasterTask.setUserId(oldMasterTask.getUserId());
            newMasterTask.setCategoryId(oldMasterTask.getCategoryId());
            newMasterTask.setTitle(oldMasterTask.getTitle());
            newMasterTask.setDescription(oldMasterTask.getDescription());
            newMasterTask.setStatus(oldMasterTask.getStatus());
            newMasterTask.setPriority(oldMasterTask.getPriority());
            newMasterTask.setDueDate(splitDate);
            newMasterTask.setStartDate(splitDate);
            newMasterTask.setIsMaster(true);
            
            int newMasterTaskId = taskDAO.insertAndGetId(newMasterTask);
            if (newMasterTaskId <= 0) {
                Log.e(TAG, "Không thể tạo master task mới");
                return false;
            }
            
            // Tạo recurrence mới cho master task mới
            TaskRecurrence newRecurrence = new TaskRecurrence();
            newRecurrence.setTaskId(newMasterTaskId);
            newRecurrence.setRecurrenceType(oldRecurrence.getRecurrenceType());
            newRecurrence.setRecurrenceInterval(oldRecurrence.getRecurrenceInterval());
            newRecurrence.setRecurrenceDays(oldRecurrence.getRecurrenceDays());
            newRecurrence.setRecurrenceDayOfMonth(oldRecurrence.getRecurrenceDayOfMonth());
            newRecurrence.setRecurrenceEndDate(oldRecurrence.getRecurrenceEndDate()); // Giữ nguyên end date
            newRecurrence.setRecurrenceCount(oldRecurrence.getRecurrenceCount());
            newRecurrence.setActive(true);
            
            return recurrenceDAO.insert(newRecurrence);
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi split recurrence: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lấy ngày hôm nay dưới dạng string (yyyy-MM-dd).
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}

