package tna.cntt2.appquanlychitieu;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class RegisterActivity extends AppCompatActivity {

    private FinancialViewModel viewModel;
    private EditText edtEmail, edtPassword, edtConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtEmail = findViewById(R.id.edtRegisterEmail);
        edtPassword = findViewById(R.id.edtRegisterPassword);
        edtConfirmPassword = findViewById(R.id.edtRegisterConfirmPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvToLogin = findViewById(R.id.tvToLogin);

        viewModel = new ViewModelProvider(this).get(FinancialViewModel.class);

        btnRegister.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            // Ràng buộc dữ liệu đầu vào cơ bản
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu nhập lại không trùng khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Mật khẩu bảo mật phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi ViewModel xử lý nghiệp vụ tạo user trên Firebase Auth & Firestore
            viewModel.register(email, password);
        });

        // Quay lại màn hình đăng nhập trước đó
        tvToLogin.setOnClickListener(v -> finish());

        // Lắng nghe phản hồi kết quả Đăng ký từ Repository đẩy qua ViewModel
        viewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                if (msg.equals("Đăng ký tài khoản thành công!")) {
                    // Firebase tự động đăng nhập luôn sau khi đăng ký thành công
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finishAffinity(); // Xóa sạch bộ nhớ Stack các màn hình cũ để tránh quay lại
                }
            }
        });
    }
}