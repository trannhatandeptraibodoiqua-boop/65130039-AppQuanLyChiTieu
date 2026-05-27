package tna.cntt2.appquanlychitieu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView recyclerViewCalendar;
    private TextView tvNoData;

    // Khai báo thêm các View hiển thị tóm tắt số tiền trong ngày
    private TextView tvDayIncome, tvDayExpense, tvDayTotal;

    private TransactionAdapter adapter;
    private List<TransactionModel> transactionList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedDateStr = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View cũ và mới
        calendarView = view.findViewById(R.id.calendarView);
        recyclerViewCalendar = view.findViewById(R.id.recyclerViewCalendar);
        tvNoData = view.findViewById(R.id.tvNoData);

        tvDayIncome = view.findViewById(R.id.tvDayIncome);
        tvDayExpense = view.findViewById(R.id.tvDayExpense);
        tvDayTotal = view.findViewById(R.id.tvDayTotal);

        // 2. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        transactionList = new ArrayList<>();

        // 3. Khởi tạo Adapter
        adapter = new TransactionAdapter(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onDeleteClick(TransactionModel transaction) {
                deleteTransaction(transaction);
            }
        });

        recyclerViewCalendar.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCalendar.setAdapter(adapter);

        // 4. Mặc định load ngày hôm nay khi mở tab
        Calendar calendar = Calendar.getInstance();
        selectedDateStr = String.format("%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        loadTransactionsByDate(selectedDateStr);

        // 5. Lắng nghe sự kiện đổi ngày trên Lịch
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                selectedDateStr = String.format("%02d/%02d/%d", dayOfMonth, (month + 1), year);
                loadTransactionsByDate(selectedDateStr);
            }
        });
    }

    private void loadTransactionsByDate(String targetDate) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    // Các biến cục bộ phục vụ thuật toán cộng dồn dòng tiền
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

                                    // Thuật toán phân loại và tính tổng số tiền trong ngày chọn
                                    if ("INCOME".equals(model.getType())) {
                                        totalIncome += model.getAmount();
                                    } else if ("EXPENSE".equals(model.getType())) {
                                        totalExpense += model.getAmount();
                                    }
                                }
                            }
                        }
                    }

                    // Đổ dữ liệu tổng hợp đã tính toán lên giao diện thanh tóm tắt
                    tvDayIncome.setText(String.format("%,.0f đ", totalIncome));
                    tvDayExpense.setText(String.format("%,.0f đ", totalExpense));

                    double dayTotal = totalIncome - totalExpense;
                    tvDayTotal.setText(String.format("%,.0f đ", dayTotal));

                    // Logic xử lý đổi màu sắc dựa theo trị số âm/dương của tổng tiền ngày
                    if (dayTotal >= 0) {
                        tvDayTotal.setTextColor(android.graphics.Color.parseColor("#1E3C72")); // Màu xanh dương nếu dư
                    } else {
                        tvDayTotal.setTextColor(android.graphics.Color.parseColor("#E53935")); // Màu đỏ nếu thâm hụt
                    }

                    // Cập nhật trạng thái hiển thị danh sách dòng
                    if (transactionList.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                        tvNoData.setText("Không có giao dịch nào trong ngày " + targetDate);
                        recyclerViewCalendar.setVisibility(View.GONE);
                    } else {
                        tvNoData.setVisibility(View.GONE);
                        recyclerViewCalendar.setVisibility(View.VISIBLE);
                        adapter.updateData(transactionList);
                    }
                })
                .addOnFailureListener(e -> {
                    tvNoData.setVisibility(View.VISIBLE);
                    tvNoData.setText("Lỗi kết nối khi tải dữ liệu!");
                });
    }

    private void deleteTransaction(TransactionModel transaction) {
        String docId = transaction.getTransactionId();
        if (docId == null || docId.isEmpty()) return;

        db.collection("transactions").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    loadTransactionsByDate(selectedDateStr);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Xóa thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                });
    }
}