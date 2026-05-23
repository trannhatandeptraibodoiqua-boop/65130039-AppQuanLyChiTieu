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

    private TransactionAdapter adapter;
    private List<TransactionModel> transactionList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedDateStr = ""; // Lưu ngày đang chọn định dạng dd/MM/yyyy

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View
        calendarView = view.findViewById(R.id.calendarView);
        recyclerViewCalendar = view.findViewById(R.id.recyclerViewCalendar);
        tvNoData = view.findViewById(R.id.tvNoData);

        // 2. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        transactionList = new ArrayList<>();

        // 3. Khởi tạo Adapter theo đúng Constructor nhận Listener của bạn
        adapter = new TransactionAdapter(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onDeleteClick(TransactionModel transaction) {
                // Gọi hàm xóa giao dịch khi bấm nút thùng rác
                deleteTransaction(transaction);
            }
        });

        recyclerViewCalendar.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCalendar.setAdapter(adapter);

        // 4. Mặc định load ngày hôm nay khi vừa mở tab
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

    // Hàm lấy toàn bộ giao dịch của User và lọc theo ngày bằng thuật toán Java (Vì DB dùng Timestamp)
    private void loadTransactionsByDate(String targetDate) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel model = doc.toObject(TransactionModel.class);
                        if (model != null) {
                            // Đảm bảo model có giữ Id document phòng trường hợp trường transactionId trong Object bị trống
                            if (model.getTransactionId() == null || model.getTransactionId().isEmpty()) {
                                model.setTransactionId(doc.getId());
                            }

                            // Chuyển đổi Timestamp của giao dịch thành chuỗi dd/MM/yyyy để so sánh
                            if (model.getTimestamp() != null) {
                                String itemDate = sdf.format(model.getTimestamp().toDate());
                                if (targetDate.equals(itemDate)) {
                                    transactionList.add(model);
                                }
                            }
                        }
                    }

                    // Cập nhật trạng thái hiển thị giao diện list
                    if (transactionList.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                        tvNoData.setText("Không có giao dịch nào trong ngày " + targetDate);
                        recyclerViewCalendar.setVisibility(View.GONE);
                    } else {
                        tvNoData.setVisibility(View.GONE);
                        recyclerViewCalendar.setVisibility(View.VISIBLE);
                        adapter.updateData(transactionList); // Đẩy data vào hàm updateData xịn của bạn
                    }
                })
                .addOnFailureListener(e -> {
                    tvNoData.setVisibility(View.VISIBLE);
                    tvNoData.setText("Lỗi kết nối khi tải dữ liệu!");
                });
    }

    // Hàm xóa giao dịch chuẩn khớp với Model trường getTransactionId() của bạn
    private void deleteTransaction(TransactionModel transaction) {
        String docId = transaction.getTransactionId();
        if (docId == null || docId.isEmpty()) return;

        db.collection("transactions").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    // Làm mới lại danh sách hiển thị của ngày vừa xóa
                    loadTransactionsByDate(selectedDateStr);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Xóa thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                });
    }
}