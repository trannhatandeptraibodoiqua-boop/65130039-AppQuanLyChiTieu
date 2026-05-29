package tna.cntt2.appquanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvTotalIncome, tvTotalExpense, tvDifference, tvChartTitle, tvTimeDisplay, tvListTitle;
    private TabLayout tabLayoutReport, tabLayoutChartType;
    private ImageButton btnPrevious, btnNext;

    // Khai báo thêm phần danh sách con
    private RecyclerView rvReportTransactions;
    private TransactionAdapter transactionAdapter;
    private List<TransactionModel> filteredExpenseList = new ArrayList<>();
    private List<TransactionModel> filteredIncomeList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private boolean isMonthReport = true;
    private boolean isExpenseChartActive = true;

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
        tvListTitle = view.findViewById(R.id.tvListTitle);
        tabLayoutReport = view.findViewById(R.id.tabLayoutReport);
        tabLayoutChartType = view.findViewById(R.id.tabLayoutChartType);
        tvTimeDisplay = view.findViewById(R.id.tvTimeDisplay);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        rvReportTransactions = view.findViewById(R.id.rvReportTransactions);

        // 2. Cấu hình RecyclerView cho danh sách giao dịch con
        transactionAdapter = new TransactionAdapter(this::deleteTransaction);
        rvReportTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReportTransactions.setAdapter(transactionAdapter);

        // 3. Khởi tạo dữ liệu gốc
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        reportCalendar = Calendar.getInstance();

        // 4. Tải dữ liệu mặc định
        loadReportData();

        // 5. Lắp bộ lắng nghe Tab Lọc Thời Gian (Tháng / Năm)
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

        // Lắp bộ lắng nghe Tab loại biểu đồ (Chi tiêu / Thu nhập)
        tabLayoutChartType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isExpenseChartActive = (tab.getPosition() == 0);
                updateChartDisplay();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 6. Điều hướng nút bấm lùi/tiến thời gian
        btnPrevious.setOnClickListener(v -> {
            reportCalendar.add(isMonthReport ? Calendar.MONTH : Calendar.YEAR, -1);
            loadReportData();
        });

        btnNext.setOnClickListener(v -> {
            reportCalendar.add(isMonthReport ? Calendar.MONTH : Calendar.YEAR, 1);
            loadReportData();
        });
    }

    private void loadReportData() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (userId.isEmpty()) return;

        int selectedMonth = reportCalendar.get(Calendar.MONTH);
        int selectedYear = reportCalendar.get(Calendar.YEAR);

        if (isMonthReport) {
            String monthText = String.format(Locale.getDefault(), "%02d/%d", (selectedMonth + 1), selectedYear);
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

                    expenseByCategoryMap.clear();
                    incomeByCategoryMap.clear();
                    filteredExpenseList.clear();
                    filteredIncomeList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TransactionModel model = doc.toObject(TransactionModel.class);

                        if (model != null && model.getTimestamp() != null) {
                            if (model.getTransactionId() == null || model.getTransactionId().isEmpty()) {
                                model.setTransactionId(doc.getId());
                            }

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
                                    filteredIncomeList.add(model); // Thêm vào list thu con

                                    if (incomeByCategoryMap.containsKey(category)) {
                                        incomeByCategoryMap.put(category, incomeByCategoryMap.get(category) + model.getAmount());
                                    } else {
                                        incomeByCategoryMap.put(category, model.getAmount());
                                    }
                                } else {
                                    totalExpense += model.getAmount();
                                    filteredExpenseList.add(model); // Thêm vào list chi con

                                    if (expenseByCategoryMap.containsKey(category)) {
                                        expenseByCategoryMap.put(category, expenseByCategoryMap.get(category) + model.getAmount());
                                    } else {
                                        expenseByCategoryMap.put(category, model.getAmount());
                                    }
                                }
                            }
                        }
                    }

                    tvTotalIncome.setText(String.format(Locale.getDefault(), "+%,.0f đ", totalIncome));
                    tvTotalExpense.setText(String.format(Locale.getDefault(), "-%,.0f đ", totalExpense));

                    double difference = totalIncome - totalExpense;
                    if (difference >= 0) {
                        tvDifference.setText(String.format(Locale.getDefault(), "+%,.0f đ", difference));
                        tvDifference.setTextColor(Color.parseColor("#1E3C72"));
                    } else {
                        tvDifference.setText(String.format(Locale.getDefault(), "%,.0f đ", difference));
                        tvDifference.setTextColor(Color.parseColor("#C62828"));
                    }

                    updateChartDisplay();
                });
    }

    private void updateChartDisplay() {
        String timeText = tvTimeDisplay.getText().toString();

        if (isExpenseChartActive) {
            tvChartTitle.setText(isMonthReport ? "Cấu trúc chi tiêu tháng " + timeText : "Cấu trúc chi tiêu cả năm " + timeText);
            tvListTitle.setText("Chi tiết các khoản chi tiêu");
            drawPieChart(expenseByCategoryMap, "Chi tiêu");

            // Đổ danh sách khoản chi vào adapter
            transactionAdapter.updateData(filteredExpenseList);
        } else {
            tvChartTitle.setText(isMonthReport ? "Cấu trúc thu nhập tháng " + timeText : "Cấu trúc thu nhập cả năm " + timeText);
            tvListTitle.setText("Chi tiết các khoản thu nhập");
            drawPieChart(incomeByCategoryMap, "Thu nhập");

            // Đổ danh sách khoản thu vào adapter
            transactionAdapter.updateData(filteredIncomeList);
        }
    }

    private void drawPieChart(HashMap<String, Double> dataMap, String labelType) {
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("Không có dữ liệu " + labelType.toLowerCase() + " để hiển thị!");
            pieChart.setNoDataTextColor(Color.GRAY);
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        int[] premiumColors = new int[] {
                Color.parseColor("#1E3C72"), Color.parseColor("#2E7D32"),
                Color.parseColor("#FF9800"), Color.parseColor("#E53935"),
                Color.parseColor("#00ACC1"), Color.parseColor("#8E24AA")
        };
        dataSet.setColors(premiumColors);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        dataSet.setValueLineColor(Color.parseColor("#757575"));
        dataSet.setValueLineWidth(1.2f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.4f);

        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#1E3C72"));

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setDescription(null);

        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.parseColor("#424242"));
        pieChart.setEntryLabelTextSize(11f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setCenterText("Tỷ lệ\n" + labelType);
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#1E3C72"));

        pieChart.setExtraOffsets(25f, 10f, 25f, 10f);

        com.github.mikephil.charting.components.Legend legend = pieChart.getLegend();
        legend.setTextSize(11f);
        legend.setTextColor(Color.parseColor("#616161"));
        legend.setWordWrapEnabled(true);

        pieChart.animateY(600);
        pieChart.invalidate();
    }

    private void deleteTransaction(TransactionModel transaction) {
        String docId = transaction.getTransactionId();
        if (docId == null || docId.isEmpty()) return;

        db.collection("transactions").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Xóa giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    loadReportData(); // Reload toàn bộ dữ liệu báo cáo sau khi xóa thành công
                });
    }
}