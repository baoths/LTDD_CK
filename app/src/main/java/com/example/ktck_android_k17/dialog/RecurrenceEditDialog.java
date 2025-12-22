package com.example.ktck_android_k17.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.example.ktck_android_k17.R;

/**
 * Dialog để chọn cách chỉnh sửa/xóa recurring task.
 * Hiển thị 3 options:
 * 1. Chỉ lần này (This occurrence only)
 * 2. Từ lần này trở đi (From this occurrence forward)
 * 3. Tất cả các lần (All occurrences)
 */
public class RecurrenceEditDialog extends Dialog {

    public static final int OPTION_THIS_OCCURRENCE = 1;
    public static final int OPTION_FROM_THIS_FORWARD = 2;
    public static final int OPTION_ALL_OCCURRENCES = 3;

    private OnOptionSelectedListener listener;
    private CardView cardThisOccurrence;
    private CardView cardFromThisForward;
    private CardView cardAllOccurrences;
    private Button btnCancel;

    public interface OnOptionSelectedListener {
        void onOptionSelected(int option);
    }

    public RecurrenceEditDialog(@NonNull Context context) {
        super(context);
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_recurrence_edit);

        cardThisOccurrence = findViewById(R.id.cardThisOccurrence);
        cardFromThisForward = findViewById(R.id.cardFromThisForward);
        cardAllOccurrences = findViewById(R.id.cardAllOccurrences);
        btnCancel = findViewById(R.id.btnCancel);

        if (cardThisOccurrence != null) {
            cardThisOccurrence.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOptionSelected(OPTION_THIS_OCCURRENCE);
                }
                dismiss();
            });
        }

        if (cardFromThisForward != null) {
            cardFromThisForward.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOptionSelected(OPTION_FROM_THIS_FORWARD);
                }
                dismiss();
            });
        }

        if (cardAllOccurrences != null) {
            cardAllOccurrences.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOptionSelected(OPTION_ALL_OCCURRENCES);
                }
                dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }
    }
}

