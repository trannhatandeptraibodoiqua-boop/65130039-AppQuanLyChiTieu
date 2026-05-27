package tna.cntt2.appquanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private GridView gridViewCalendar;
    private TextView tvMonthYear, tvNoData;
    private ImageButton btnPrevMonth, btnNextMonth;
    private RecyclerView recyclerViewCalendar;
    private TextView tvDayIncome, tvDayExpense, tvDayTotal;

    private TransactionAdapter adapter;
    private List<TransactionModel> transactionList;
    private FirebaseFirestore db;
    private String currentUid;
    private String selectedDateStr = "";

    private Calendar currentMonthCalendar = Calendar.getInstance(); // Lưu tháng đang xem
    private HashSet<String> allEventDates = new HashSet<>(); // Lưu tất cả các ngày có giao dịch
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View
        gridViewCalendar = view.findViewById(R.id.gridViewCalendar);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        recyclerViewCalendar = view.findViewById(R.id.recyclerViewCalendar);
        tvNoData = view.findViewById(R.id.tvNoData);
        tvDayIncome = view.findViewById(R.id.tvDayIncome);
        tvDayExpense = view.findViewById(R.id.tvDayExpense);
        tvDayTotal = view.findViewById(R.id.tvDayTotal);

        db = FirebaseFirestore.getInstance();
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        currentUid = (user != null) ? user.getUid() : "";
        transactionList = new ArrayList<>();

        adapter = new TransactionAdapter(this::deleteTransaction);
        recyclerViewCalendar.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCalendar.setAdapter(adapter);

        // Mặc định chọn ngày hôm nay
        Calendar today = Calendar.getInstance();
        selectedDateStr = sdf.format(today.getTime());

        // Lắng nghe nút bấm chuyển tháng
        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            updateCalendarMatrix();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateCalendarMatrix();
        });

        // Click chọn một ô ngày bất kỳ trên lưới lịch
        gridViewCalendar.setOnItemClickListener((parent, view1, position, id) -> {
            String clickedDate = (String) parent.getItemAtPosition(position);
            if (!clickedDate.isEmpty()) {
                selectedDateStr = clickedDate;
                updateCalendarMatrix(); // Vẽ lại để cập nhật màu nền ô được chọn
                loadTransactionsByDate(selectedDateStr); // Tải list chi tiết
            }
        });

        // Tải toàn bộ mốc thời gian để chấm chấm lên lịch, đồng thời load list ngày hiện tại
        getAllTransactionDates();
    }

    // Quét Firestore lấy các ngày có giao dịch để bỏ vào HashSet tốc độ cao
    private void getAllTransactionDates() {
        if (currentUid.isEmpty()) return;
        db.collection("transactions")
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEventDates.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel model = doc.toObject(TransactionModel.class);
                        if (model != null && model.getTimestamp() != null) {
                            allEventDates.add(sdf.format(model.getTimestamp().toDate()));
                        }
                    }
                    updateCalendarMatrix();
                    loadTransactionsByDate(selectedDateStr);
                });
    }

    // Thuật toán dựng ma trận lịch tháng thủ công
    private void updateCalendarMatrix() {
        ArrayList<String> daysInMonth = new ArrayList<>();

        // Thiết lập tiêu đề tháng/năm
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("'Tháng' MM/yyyy", Locale.getDefault());
        tvMonthYear.setText(monthYearFormat.format(currentMonthCalendar.getTime()));

        Calendar tempCal = (Calendar) currentMonthCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1); // Đưa về ngày mùng 1 đầu tháng

        // Lấy xem ngày mùng 1 đầu tháng rơi vào thứ mấy (Chủ nhật = 1, Thứ 2 = 2, ...)
        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        int blankSpaces = dayOfWeek - 1; // Số ô trống cần chèn trước ngày mùng 1

        for (int i = 0; i < blankSpaces; i++) {
            daysInMonth.add(""); // Ô trống
        }

        int totalDaysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= totalDaysInMonth; i++) {
            daysInMonth.add(String.format(Locale.getDefault(), "%02d/%02d/%d",
                    i,
                    currentMonthCalendar.get(Calendar.MONTH) + 1,
                    currentMonthCalendar.get(Calendar.YEAR)));
        }

        CustomCalendarAdapter calendarAdapter = new CustomCalendarAdapter(getContext(), daysInMonth, allEventDates, selectedDateStr);
        gridViewCalendar.setAdapter(calendarAdapter);
    }

    private void loadTransactionsByDate(String targetDate) {
        if (currentUid.isEmpty()) return;
        db.collection("transactions")
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel model = doc.toObject(TransactionModel.class);
                        if (model != null) {
                            if (model.getTransactionId() == null || model.getTransactionId().isEmpty()) {
                                model.setTransactionId(doc.getId());
                            }
                            if (model.getTimestamp() != null) {
                                String itemDate = sdf.format(model.getTimestamp().toDate());
                                if (targetDate.equals(itemDate)) {
                                    transactionList.add(model);
                                    if ("INCOME".equals(model.getType())) totalIncome += model.getAmount();
                                    else if ("EXPENSE".equals(model.getType())) totalExpense += model.getAmount();
                                }
                            }
                        }
                    }

                    tvDayIncome.setText(String.format(Locale.getDefault(), "%,.0f đ", totalIncome));
                    tvDayExpense.setText(String.format(Locale.getDefault(), "%,.0f đ", totalExpense));
                    double dayTotal = totalIncome - totalExpense;
                    tvDayTotal.setText(String.format(Locale.getDefault(), "%,.0f đ", dayTotal));
                    tvDayTotal.setTextColor(Color.parseColor(dayTotal >= 0 ? "#1E3C72" : "#E53935"));

                    if (transactionList.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                        tvNoData.setText("Không có giao dịch nào trong ngày " + targetDate);
                        recyclerViewCalendar.setVisibility(View.GONE);
                    } else {
                        tvNoData.setVisibility(View.GONE);
                        recyclerViewCalendar.setVisibility(View.VISIBLE);
                        adapter.updateData(transactionList);
                    }
                });
    }

    private void deleteTransaction(TransactionModel transaction) {
        String docId = transaction.getTransactionId();
        if (docId == null || docId.isEmpty()) return;

        db.collection("transactions").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    getAllTransactionDates(); // Quét lại để cập nhật dấu chấm (nếu ngày đó hết sạch đơn)
                });
    }
}