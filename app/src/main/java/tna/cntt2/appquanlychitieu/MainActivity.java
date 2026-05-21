package tna.cntt2.appquanlychitieu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FinancialViewModel viewModel;
    private TextView tvBalance, tvBudgetInfo;
    private ProgressBar progressBudget;
    private TransactionAdapter adapter;
    private String currentUid;
    private Calendar selectedDate = Calendar.getInstance();

    private Button btnSetBudget;
    private Button btnLogout;

    private double currentMonthlyBudget = 0;
    private List<TransactionModel> currentTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra phiên đăng nhập (Không có lệnh đăng xuất tự động ở đây để tránh văng app)
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        currentUid = firebaseUser.getUid();

        tvBalance = findViewById(R.id.tvBalance);
        tvBudgetInfo = findViewById(R.id.tvBudgetInfo);
        progressBudget = findViewById(R.id.progressBudget);
        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);
        Button btnOpenAddDialog = findViewById(R.id.btnOpenAddDialog);
        btnSetBudget = findViewById(R.id.btnSetBudget);
        btnLogout = findViewById(R.id.btnLogout);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TransactionAdapter(transaction -> viewModel.removeTransaction(transaction));
        rvTransactions.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(FinancialViewModel.class);
        if (!currentUid.isEmpty()) {
            viewModel.initUserData(currentUid);
        }

        viewModel.getUserLiveData().observe(this, userModel -> {
            if (userModel != null) {
                tvBalance.setText(String.format("Số dư ví: %,.0f VNĐ", userModel.getTotalBalance()));
                currentMonthlyBudget = userModel.getMonthlyBudget();

                if (currentMonthlyBudget > 0) {
                    progressBudget.setMax((int) currentMonthlyBudget);
                }
                calculateAndFormatBudget();
            }
        });

        viewModel.getTransactionsLiveData().observe(this, transactionList -> {
            if (transactionList != null) {
                currentTransactions = transactionList;
                adapter.updateData(transactionList);
                calculateAndFormatBudget();
            }
        });

        viewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        btnOpenAddDialog.setOnClickListener(v -> showAddTransactionDialog());
        btnSetBudget.setOnClickListener(v -> showSetBudgetDialog());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void calculateAndFormatBudget() {
        double totalExpense = 0;
        for (TransactionModel trans : currentTransactions) {
            if ("EXPENSE".equals(trans.getType())) {
                totalExpense += trans.getAmount();
            }
        }

        progressBudget.setProgress((int) totalExpense);

        if (currentMonthlyBudget > 0) {
            tvBudgetInfo.setText(String.format("Đã chi: %,.0f / %,.0f VNĐ", totalExpense, currentMonthlyBudget));

            if (totalExpense > currentMonthlyBudget) {
                Toast.makeText(this, "Cảnh báo: Bạn đã vượt quá định mức chi tiêu tháng!", Toast.LENGTH_LONG).show();
                progressBudget.setProgressTintList(ColorStateList.valueOf(Color.RED));
            } else {
                progressBudget.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            }
        } else {
            tvBudgetInfo.setText("Đã chi: " + String.format("%,.0f VNĐ (Chưa thiết lập hạn mức)", totalExpense));
            progressBudget.setProgressTintList(null);
        }
    }

    private void showSetBudgetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thiết lập hạn mức chi tiêu tháng");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Nhập số tiền (Ví dụ: 5000000)");

        input.setPadding(50, 40, 50, 40);
        builder.setView(input);

        builder.setPositiveButton("LƯU", (dialog, which) -> {
            String budgetStr = input.getText().toString().trim();
            if (!budgetStr.isEmpty()) {
                double amount = Double.parseDouble(budgetStr);
                viewModel.setMonthlyBudget(currentUid, amount);
            } else {
                Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("HỦY", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        EditText edtAmount = dialogView.findViewById(R.id.edtAmount);
        RadioGroup rgType = dialogView.findViewById(R.id.rgType);
        Spinner spnCategory = dialogView.findViewById(R.id.spnCategory);
        Button btnDatePicker = dialogView.findViewById(R.id.btnDatePicker);
        EditText edtNote = dialogView.findViewById(R.id.edtNote);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTransaction);

        btnDatePicker.setText(selectedDate.get(Calendar.DAY_OF_MONTH) + "/" + (selectedDate.get(Calendar.MONTH) + 1) + "/" + selectedDate.get(Calendar.YEAR));

        String[] categories = {"Ăn uống", "Mua sắm", "Di chuyển", "Lương thưởng", "Giải trí", "Y tế"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spnCategory.setAdapter(spinnerAdapter);

        btnDatePicker.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                btnDatePicker.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String amountStr = edtAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "INCOME" : "EXPENSE";
            String category = spnCategory.getSelectedItem().toString();
            String note = edtNote.getText().toString().trim();

            TransactionModel newTrans = new TransactionModel(
                    "", currentUid, amount, type, category, new Timestamp(selectedDate.getTime()), note
            );

            viewModel.addTransaction(newTrans);
            dialog.dismiss();

            selectedDate = Calendar.getInstance();
        });

        dialog.show();
    }
}