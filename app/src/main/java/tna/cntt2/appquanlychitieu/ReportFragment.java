package tna.cntt2.appquanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvTotalIncome, tvTotalExpense, tvChartTitle, tvTimeDisplay;
    private TabLayout tabLayoutReport;
    private ImageButton btnPrevious, btnNext;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private boolean isMonthReport = true;

    // Biến lưu trữ mốc thời gian người dùng đang đứng xem (Có thể tăng giảm bằng nút bấm)
    private Calendar reportCalendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View
        pieChart = view.findViewById(R.id.pieChart);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvChartTitle = view.findViewById(R.id.tvChartTitle);
        tabLayoutReport = view.findViewById(R.id.tabLayoutReport);
        tvTimeDisplay = view.findViewById(R.id.tvTimeDisplay);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);

        // 2. Khởi tạo Firebase & Mốc thời gian mặc định ban đầu (Hôm nay)
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        reportCalendar = Calendar.getInstance();

        // 3. Tải dữ liệu lần đầu tiên
        loadReportData();

        // 4. Lắng nghe chuyển Tab (Tháng / Năm)
        tabLayoutReport.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isMonthReport = (tab.getPosition() == 0);
                loadReportData();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 5. Lắng nghe nút QUAY VỀ thời gian trước (<)
        btnPrevious.setOnClickListener(v -> {
            if (isMonthReport) {
                reportCalendar.add(Calendar.MONTH, -1); // Lùi lại 1 tháng
            } else {
                reportCalendar.add(Calendar.YEAR, -1); // Lùi lại 1 năm
            }
            loadReportData();
        });

        // 6. Lắng nghe nút TIẾN TỚI thời gian sau (>)
        btnNext.setOnClickListener(v -> {
            if (isMonthReport) {
                reportCalendar.add(Calendar.MONTH, 1); // Tiến lên 1 tháng
            } else {
                reportCalendar.add(Calendar.YEAR, 1); // Tiến lên 1 năm
            }
            loadReportData();
        });
    }

    private void loadReportData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        // Bóc tách Tháng / Năm từ đối tượng lịch đang được chọn
        int selectedMonth = reportCalendar.get(Calendar.MONTH);
        int selectedYear = reportCalendar.get(Calendar.YEAR);

        // Cập nhật text hiển thị ở giữa hai nút mũi tên điều hướng và tiêu đề biểu đồ
        if (isMonthReport) {
            String monthText = String.format("%02d/%d", (selectedMonth + 1), selectedYear);
            tvTimeDisplay.setText(monthText);
            tvChartTitle.setText("Cấu trúc chi tiêu tháng " + monthText);
        } else {
            String yearText = String.valueOf(selectedYear);
            tvTimeDisplay.setText(yearText);
            tvChartTitle.setText("Cấu trúc chi tiêu cả năm " + yearText);
        }

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    double totalIncome = 0;
                    double totalExpense = 0;
                    HashMap<String, Double> expenseByCategoryMap = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel model = doc.toObject(TransactionModel.class);

                        if (model != null && model.getTimestamp() != null) {
                            Calendar itemCal = Calendar.getInstance();
                            itemCal.setTime(model.getTimestamp().toDate());

                            int itemMonth = itemCal.get(Calendar.MONTH);
                            int itemYear = itemCal.get(Calendar.YEAR);

                            // Lọc dữ liệu chuẩn xác theo Tháng/Năm được chọn trên thanh điều hướng
                            boolean matchesFilter = false;
                            if (isMonthReport) {
                                matchesFilter = (itemMonth == selectedMonth && itemYear == selectedYear);
                            } else {
                                matchesFilter = (itemYear == selectedYear);
                            }

                            if (matchesFilter) {
                                if ("INCOME".equals(model.getType())) {
                                    totalIncome += model.getAmount();
                                } else {
                                    totalExpense += model.getAmount();

                                    String category = model.getCategory() != null ? model.getCategory() : "Khác";
                                    if (expenseByCategoryMap.containsKey(category)) {
                                        expenseByCategoryMap.put(category, expenseByCategoryMap.get(category) + model.getAmount());
                                    } else {
                                        expenseByCategoryMap.put(category, model.getAmount());
                                    }
                                }
                            }
                        }
                    }

                    // Cập nhật số liệu tiền tệ lên giao diện
                    tvTotalIncome.setText(String.format("%,.0f đ", totalIncome));
                    tvTotalExpense.setText(String.format("%,.0f đ", totalExpense));

                    // Vẽ lại biểu đồ tròn
                    drawPieChart(expenseByCategoryMap);
                });
    }

    private void drawPieChart(HashMap<String, Double> expenseMap) {
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : expenseMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText(isMonthReport ? "Không có dữ liệu chi tiêu trong tháng này!" : "Không có dữ liệu chi tiêu trong năm nay!");
            pieChart.setNoDataTextColor(Color.GRAY);
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setDescription(null);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText(isMonthReport ? "Tỷ lệ\nTheo Tháng" : "Tỷ lệ\nTheo Năm");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#1E3C72"));

        pieChart.animateY(600);
        pieChart.invalidate();
    }
}