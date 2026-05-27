package tna.cntt2.appquanlychitieu;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;

public class CustomCalendarAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> daysInMonth;
    private HashSet<String> eventDates;
    private String selectedDateStr;

    public CustomCalendarAdapter(Context context, ArrayList<String> daysInMonth, HashSet<String> eventDates, String selectedDateStr) {
        this.context = context;
        this.daysInMonth = daysInMonth;
        this.eventDates = eventDates;
        this.selectedDateStr = selectedDateStr;
    }

    @Override
    public int getCount() { return daysInMonth.size(); }
    @Override
    public Object getItem(int position) { return daysInMonth.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_cell, parent, false);
        }

        TextView tvDay = convertView.findViewById(R.id.tvCellDay);
        View viewDot = convertView.findViewById(R.id.viewDot);
        View layout = convertView.findViewById(R.id.cellLayout);

        String fullDate = daysInMonth.get(position);

        if (fullDate.isEmpty()) {
            tvDay.setText("");
            viewDot.setVisibility(View.INVISIBLE);
            layout.setBackgroundColor(Color.TRANSPARENT);
        } else {
            // Cắt chuỗi lấy chỉ số ngày hiển thị lên ô (Ví dụ "27/05/2026" -> lấy "27")
            String dayNum = fullDate.split("/")[0];
            tvDay.setText(String.valueOf(Integer.parseInt(dayNum)));

            // 1. Kiểm tra nếu ngày này trùng ngày đang được chọn -> Đổi màu nền nổi bật
            if (fullDate.equals(selectedDateStr)) {
                layout.setBackgroundColor(Color.parseColor("#E0F7FA"));
                tvDay.setTextColor(Color.parseColor("#1E3C72"));
                tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                layout.setBackgroundColor(Color.TRANSPARENT);
                tvDay.setTextColor(Color.parseColor("#333333"));
                tvDay.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            // 2. THUẬT TOÁN QUAN TRỌNG: Nếu ngày này có giao dịch -> Hiện dấu chấm hệ thống lên!
            if (eventDates != null && eventDates.contains(fullDate)) {
                viewDot.setVisibility(View.VISIBLE);
            } else {
                viewDot.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }
}