package com.simplemobiletools.calendar;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import org.joda.time.DateTime;

public class MyDatePickerDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getActivity().getLayoutInflater().inflate(R.layout.date_picker, null);
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
        hideDayPicker(datePicker);

        final Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(Constants.DATE)) {
            final DateTime dateTime = new DateTime(bundle.getString(Constants.DATE));
            datePicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, 1, null);
        }

        builder.setView(view);
        view.findViewById(R.id.date_picker_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatePickedListener listener = (DatePickedListener) getActivity();
                final int month = datePicker.getMonth() + 1;
                final int year = datePicker.getYear();
                listener.onDatePicked(new DateTime().withMonthOfYear(month).withYear(year));
                dismiss();
            }
        });
        return builder.create();
    }

    private void hideDayPicker(DatePicker datePicker) {
        final LinearLayout ll = (LinearLayout) datePicker.getChildAt(0);
        final LinearLayout ll2 = (LinearLayout) ll.getChildAt(0);
        ll2.getChildAt(0).setVisibility(View.GONE);
    }

    public interface DatePickedListener {
        void onDatePicked(DateTime dateTime);
    }
}
