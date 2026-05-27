package tna.cntt2.appquanlychitieu;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;

public class InputFragment extends Fragment {

    private FinancialViewModel viewModel;
    private String currentUid;
    private Calendar selectedDate = Calendar.getInstance();

    private EditText edtAmount, edtNote;
    private RadioGroup rgType;
    private Spinner spnCategory;
    private Button btnDatePicker, btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy UID người dùng hiện tại
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
        }

        // Ánh xạ View
        edtAmount = view.findViewById(R.id.edtAmount);
        rgType = view.findViewById(R.id.rgType);
        spnCategory = view.findViewById(R.id.spnCategory);
        btnDatePicker = view.findViewById(R.id.btnDatePicker);
        edtNote = view.findViewById(R.id.edtNote);
        btnSave = view.findViewById(R.id.btnSaveTransaction);

        // Gọi chung ViewModel với Activity để tự động đồng bộ số liệu sang tab Home
        viewModel = new ViewModelProvider(requireActivity()).get(FinancialViewModel.class);

        updateDateButtonText();

        // Khai báo mảng danh mục dữ liệu của bạn
        String[] expenseCategories = {"Ăn uống", "Mua sắm", "Di chuyển", "Giải trí", "Y tế", "Nhà cửa"};
        String[] incomeCategories = {"Lương thưởng", "Làm thêm (Freelance)", "Được tặng", "Tiền lãi / Đầu tư"};

        // Xử lý đổi danh mục động theo Thu / Chi
        Runnable updateSpinnerCategories = () -> {
            boolean isIncome = rgType.getCheckedRadioButtonId() == R.id.rbIncome;
            String[] targetCategories = isIncome ? incomeCategories : expenseCategories;

            // Đổi màu số tiền (Thu -> Xanh lá, Chi -> Đỏ)
            edtAmount.setTextColor(Color.parseColor(isIncome ? "#4CAF50" : "#E53935"));

            if (getContext() != null) {
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, targetCategories);
                spnCategory.setAdapter(spinnerAdapter);
            }
        };

        updateSpinnerCategories.run();
        rgType.setOnCheckedChangeListener((group, checkedId) -> updateSpinnerCategories.run());

        // Chọn ngày
        btnDatePicker.setOnClickListener(v -> {
            if (getContext() == null) return;
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                updateDateButtonText();
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Bấm lưu giao dịch lên Firestore
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

            Toast.makeText(getContext(), "Thêm giao dịch thành công!", Toast.LENGTH_SHORT).show();
            clearForm();
        });
    }

    private void updateDateButtonText() {
        btnDatePicker.setText("Ngày thực hiện: " + selectedDate.get(Calendar.DAY_OF_MONTH) + "/" + (selectedDate.get(Calendar.MONTH) + 1) + "/" + selectedDate.get(Calendar.YEAR));
    }

    private void clearForm() {
        edtAmount.setText("");
        edtNote.setText("");
        selectedDate = Calendar.getInstance();
        updateDateButtonText();
        rgType.check(R.id.rbExpense);
    }
}