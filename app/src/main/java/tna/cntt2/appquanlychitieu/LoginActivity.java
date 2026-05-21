package tna.cntt2.appquanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FinancialViewModel viewModel;
    private EditText edtEmail, edtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Giúp bạn luôn luôn được nhập lại tài khoản/mật khẩu mỗi khi Rebuild/Run app để test bài
        FirebaseAuth.getInstance().signOut();

        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtLoginEmail);
        edtPassword = findViewById(R.id.edtLoginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvToRegister = findViewById(R.id.tvToRegister);

        viewModel = new ViewModelProvider(this).get(FinancialViewModel.class);

        // Xử lý sự kiện nút bấm Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin tài khoản!", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.login(email, password);
        });

        // Chuyển sang màn hình Đăng ký
        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Lắng nghe tín hiệu kết quả đăng nhập từ LiveData của ViewModel
        viewModel.getStatusMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                if (msg.equals("Đăng nhập thành công!")) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish(); // Đóng LoginActivity để không bị quay lại khi bấm nút Back
                }
            }
        });
    }
}