package tna.cntt2.appquanlychitieu;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private FinancialViewModel viewModel;
    private TextView tvBalance, tvBudgetInfo;
    private ProgressBar progressBudget;
    private TransactionAdapter adapter;
    private String currentUid;
    private Calendar selectedDate = Calendar.getInstance();
    private Button btnSetBudget, btnLogout;
    private double currentMonthlyBudget = 0;
    private List<TransactionModel> currentTransactions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
            return;
        }

        currentUid = firebaseUser.getUid();

        tvBalance = view.findViewById(R.id.tvBalance);
        tvBudgetInfo = view.findViewById(R.id.tvBudgetInfo);
        progressBudget = view.findViewById(R.id.progressBudget);
        RecyclerView rvTransactions = view.findViewById(R.id.rvTransactions);
        Button btnOpenAddDialog = view.findViewById(R.id.btnOpenAddDialog);
        btnSetBudget = view.findViewById(R.id.btnSetBudget);
        btnLogout = view.findViewById(R.id.btnLogout);

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(transaction -> viewModel.removeTransaction(transaction));
        rvTransactions.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(FinancialViewModel.class);
        if (!currentUid.isEmpty()) {
            viewModel.initUserData(currentUid);
        }

        viewModel.getUserLiveData().observe(getViewLifecycleOwner(), userModel -> {
            if (userModel != null) {
                tvBalance.setText(String.format("Số dư ví: %,.0f VNĐ", userModel.getTotalBalance()));
                currentMonthlyBudget = userModel.getMonthlyBudget();
                if (currentMonthlyBudget > 0) progressBudget.setMax((int) currentMonthlyBudget);
                calculateAndFormatBudget();
            }
        });

        viewModel.getTransactionsLiveData().observe(getViewLifecycleOwner(), transactionList -> {
            if (transactionList != null) {
                currentTransactions = transactionList;
                adapter.updateData(transactionList);
                calculateAndFormatBudget();
            }
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        btnOpenAddDialog.setOnClickListener(v -> showAddTransactionDialog());
        btnSetBudget.setOnClickListener(v -> showSetBudgetDialog());
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });
    }

    private void calculateAndFormatBudget() {
        double totalExpense = 0;
        for (TransactionModel trans : currentTransactions) {
            if ("EXPENSE".equals(trans.getType())) totalExpense += trans.getAmount();
        }
        progressBudget.setProgress((int) totalExpense);

        if (currentMonthlyBudget > 0) {
            tvBudgetInfo.setText(String.format("Đã chi: %,.0f / %,.0f VNĐ", totalExpense, currentMonthlyBudget));
            if (totalExpense > currentMonthlyBudget) {
                if (getContext() != null) Toast.makeText(getContext(), "Cảnh báo: Bạn đã vượt quá định mức chi tiêu tháng!", Toast.LENGTH_LONG).show();
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
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Thiết lập hạn mức chi tiêu tháng");
        final EditText input = new EditText(getContext());
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
                Toast.makeText(getContext(), "Vui lòng nhập số tiền hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("HỦY", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAddTransactionDialog() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(dialogView);

        EditText edtAmount = dialogView.findViewById(R.id.edtAmount);
        RadioGroup rgType = dialogView.findViewById(R.id.rgType);
        Spinner spnCategory = dialogView.findViewById(R.id.spnCategory);
        Button btnDatePicker = dialogView.findViewById(R.id.btnDatePicker);
        EditText edtNote = dialogView.findViewById(R.id.edtNote);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTransaction);

        btnDatePicker.setText(selectedDate.get(Calendar.DAY_OF_MONTH) + "/" + (selectedDate.get(Calendar.MONTH) + 1) + "/" + selectedDate.get(Calendar.YEAR));
        String[] categories = {"Ăn uống", "Mua sắm", "Di chuyển", "Lương thưởng", "Giải trí", "Y tế"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spnCategory.setAdapter(spinnerAdapter);

        btnDatePicker.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                btnDatePicker.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> {
            String amountStr = edtAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "INCOME" : "EXPENSE";
            String category = spnCategory.getSelectedItem().toString();
            String note = edtNote.getText().toString().trim();

            TransactionModel newTrans = new TransactionModel("", currentUid, amount, type, category, new Timestamp(selectedDate.getTime()), note);
            viewModel.addTransaction(newTrans);
            dialog.dismiss();
            selectedDate = Calendar.getInstance();
        });
        dialog.show();
    }
}