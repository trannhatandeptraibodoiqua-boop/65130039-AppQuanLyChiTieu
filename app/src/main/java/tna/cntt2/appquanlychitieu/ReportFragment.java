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
    private TextView tvTotalIncome, tvTotalExpense, tvDifference, tvChartTitle, tvTimeDisplay;
    private TabLayout tabLayoutReport, tabLayoutChartType;
    private ImageButton btnPrevious, btnNext;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private boolean isMonthReport = true;
    private boolean isExpenseChartActive = true; // Theo dõi đang xem biểu đồ Chi hay Thu

    // Hai bản đồ lưu trữ danh mục tương ứng
    private HashMap<String, Double> expenseByCategoryMap = new HashMap<>();
    private HashMap<String, Double> incomeByCategoryMap = new HashMap<>();

    private Calendar reportCalendar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ View đầy đủ
        pieChart = view.findViewById(R.id.pieChart);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvDifference = view.findViewById(R.id.tvDifference);
        tvChartTitle = view.findViewById(R.id.tvChartTitle);
        tabLayoutReport = view.findViewById(R.id.tabLayoutReport);
        tabLayoutChartType = view.findViewById(R.id.tabLayoutChartType);
        tvTimeDisplay = view.findViewById(R.id.tvTimeDisplay);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);

        // 2. Khởi tạo dữ liệu gốc
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        reportCalendar = Calendar.getInstance();

        // 3. Tải dữ liệu mặc định
        loadReportData();

        // 4. Lắng nghe chuyển Tab Lọc Thời Gian (Tháng / Năm)
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

        // Lắng nghe chuyển đổi Tab loại biểu đồ (Chi tiêu / Thu nhập)
        tabLayoutChartType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isExpenseChartActive = (tab.getPosition() == 0);
                updateChartDisplay(); // Chỉ cần vẽ lại hình từ dữ liệu có sẵn, không tải lại Firebase
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 5. Điều hướng nút bấm lùi thời gian
        btnPrevious.setOnClickListener(v -> {
            if (isMonthReport) {
                reportCalendar.add(Calendar.MONTH, -1);
            } else {
                reportCalendar.add(Calendar.YEAR, -1);
            }
            loadReportData();
        });

        // 6. Điều hướng nút bấm tiến thời gian
        btnNext.setOnClickListener(v -> {
            if (isMonthReport) {
                reportCalendar.add(Calendar.MONTH, 1);
            } else {
                reportCalendar.add(Calendar.YEAR, 1);
            }
            loadReportData();
        });
    }

    private void loadReportData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        int selectedMonth = reportCalendar.get(Calendar.MONTH);
        int selectedYear = reportCalendar.get(Calendar.YEAR);

        if (isMonthReport) {
            String monthText = String.format("%02d/%d", (selectedMonth + 1), selectedYear);
            tvTimeDisplay.setText(monthText);
        } else {
            String yearText = String.valueOf(selectedYear);
            tvTimeDisplay.setText(yearText);
        }

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    double totalIncome = 0;
                    double totalExpense = 0;

                    // Xóa sạch dữ liệu cũ trước khi nạp bộ lọc mới
                    expenseByCategoryMap.clear();
                    incomeByCategoryMap.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel model = doc.toObject(TransactionModel.class);

                        if (model != null && model.getTimestamp() != null) {
                            Calendar itemCal = Calendar.getInstance();
                            itemCal.setTime(model.getTimestamp().toDate());

                            int itemMonth = itemCal.get(Calendar.MONTH);
                            int itemYear = itemCal.get(Calendar.YEAR);

                            boolean matchesFilter = isMonthReport
                                    ? (itemMonth == selectedMonth && itemYear == selectedYear)
                                    : (itemYear == selectedYear);

                            if (matchesFilter) {
                                String category = model.getCategory() != null ? model.getCategory() : "Khác";

                                if ("INCOME".equals(model.getType())) {
                                    totalIncome += model.getAmount();
                                    // Gom nhóm danh mục Thu Nhập
                                    if (incomeByCategoryMap.containsKey(category)) {
                                        incomeByCategoryMap.put(category, incomeByCategoryMap.get(category) + model.getAmount());
                                    } else {
                                        incomeByCategoryMap.put(category, model.getAmount());
                                    }
                                } else {
                                    totalExpense += model.getAmount();
                                    // Gom nhóm danh mục Chi Tiêu
                                    if (expenseByCategoryMap.containsKey(category)) {
                                        expenseByCategoryMap.put(category, expenseByCategoryMap.get(category) + model.getAmount());
                                    } else {
                                        expenseByCategoryMap.put(category, model.getAmount());
                                    }
                                }
                            }
                        }
                    }

                    // Hiển thị số tiền lên các ô chỉ số công khai
                    tvTotalIncome.setText(String.format("+%,.0f đ", totalIncome));
                    tvTotalExpense.setText(String.format("-%,.0f đ", totalExpense));

                    // Tính toán hiệu số chênh lệch thu chi công thêm màu sắc phân biệt sinh động
                    double difference = totalIncome - totalExpense;
                    if (difference >= 0) {
                        tvDifference.setText(String.format("+%,.0f đ", difference));
                        tvDifference.setTextColor(Color.parseColor("#1E3C72")); // Màu xanh dương nếu dương tiền
                    } else {
                        tvDifference.setText(String.format("%,.0f đ", difference));
                        tvDifference.setTextColor(Color.parseColor("#C62828")); // Màu đỏ nếu âm tiền
                    }

                    // Đẩy dữ liệu ra hàm phân phối biểu đồ hiển thị
                    updateChartDisplay();
                });
    }

    private void updateChartDisplay() {
        String timeText = tvTimeDisplay.getText().toString();

        if (isExpenseChartActive) {
            tvChartTitle.setText(isMonthReport ? "Cấu trúc chi tiêu tháng " + timeText : "Cấu trúc chi tiêu cả năm " + timeText);
            drawPieChart(expenseByCategoryMap, "Chi tiêu");
        } else {
            tvChartTitle.setText(isMonthReport ? "Cấu trúc thu nhập tháng " + timeText : "Cấu trúc thu nhập cả năm " + timeText);
            drawPieChart(incomeByCategoryMap, "Thu nhập");
        }
    }

    private void drawPieChart(HashMap<String, Double> dataMap, String labelType) {
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("Không có dữ liệu dữ liệu " + labelType.toLowerCase() + " để hiển thị!");
            pieChart.setNoDataTextColor(Color.GRAY);
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Bộ màu sắc tươi sáng cho các lát bánh
        int[] premiumColors = new int[] {
                Color.parseColor("#1E3C72"), // Xanh đậm
                Color.parseColor("#2E7D32"), // Xanh lá
                Color.parseColor("#FF9800"), // Cam
                Color.parseColor("#E53935"), // Đỏ
                Color.parseColor("#00ACC1"), // Xanh Teal
                Color.parseColor("#8E24AA")  // Tím
        };
        dataSet.setColors(premiumColors);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // Đẩy tên danh mục ra ngoài
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // Đẩy số phần trăm (%) ra ngoài

        // Cấu hình đường kẻ mũi tên chỉ từ chữ vào lát bánh tương ứng
        dataSet.setValueLineColor(Color.parseColor("#757575")); // Màu đường chỉ (Màu xám)
        dataSet.setValueLineWidth(1.2f); // Độ dày đường chỉ
        dataSet.setValueLinePart1OffsetPercentage(80f); // Độ dài đoạn thẳng 1
        dataSet.setValueLinePart1Length(0.4f); // Độ dài đoạn thẳng 2
        dataSet.setValueLinePart2Length(0.4f);

        // Vì chữ đã nhảy ra ngoài nền trắng, ta đổi màu chữ sang màu tối để nhìn rõ nét
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#1E3C72")); // Màu của con số % (Xanh đậm)

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setDescription(null);

        // Bật lại hiển thị chữ danh mục
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.parseColor("#424242")); // Đổi màu chữ Danh mục thành màu xám đen để nổi trên nền trắng
        pieChart.setEntryLabelTextSize(11f); // Cỡ chữ danh mục

        // Cấu hình vòng tròn rỗng ở giữa biểu đồ
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText("Tỷ lệ\n" + labelType);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#1E3C72"));

        // Khoảng cách lề xung quanh biểu đồ để chữ ở ngoài không bị cắt mất
        pieChart.setExtraOffsets(25f, 10f, 25f, 10f);

        // Bảng chú thích màu sắc ở góc dưới cùng
        com.github.mikephil.charting.components.Legend legend = pieChart.getLegend();
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#616161"));
        legend.setWordWrapEnabled(true);

        pieChart.animateY(600);
        pieChart.invalidate();
    }
}