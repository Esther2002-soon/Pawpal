package com.example.pawpal.utils

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar


fun showDateTimePicker(context: Context, initial: Calendar, onTimeSelected: (Calendar) -> Unit) {
    val year = initial.get(Calendar.YEAR)
    val month = initial.get(Calendar.MONTH)
    val day = initial.get(Calendar.DAY_OF_MONTH)
    val hour = initial.get(Calendar.HOUR_OF_DAY)
    val minute = initial.get(Calendar.MINUTE)

    DatePickerDialog(context, { _, y, m, d ->
        TimePickerDialog(context, { _, h, min ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, min)
            }
            onTimeSelected(calendar)
        }, hour, minute, true).show()
    }, year, month, day).show()
}