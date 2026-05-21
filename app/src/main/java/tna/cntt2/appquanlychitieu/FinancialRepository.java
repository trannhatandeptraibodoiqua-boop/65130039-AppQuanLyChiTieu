package tna.cntt2.appquanlychitieu;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class FinancialRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public void registerUser(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    UserModel newUser = new UserModel(uid, email, 0.0, 0.0);

                    firestore.collection("users").document(uid).set(newUser)
                            .addOnSuccessListener(aVoid -> callback.onSuccess("Đăng ký tài khoản thành công!"))
                            .addOnFailureListener(e -> callback.onFailure("Khởi tạo thông tin user thất bại: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Đăng ký tài khoản thất bại: " + e.getMessage()));
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> callback.onSuccess("Đăng nhập thành công!"))
                .addOnFailureListener(e -> callback.onFailure("Sai tài khoản hoặc mật khẩu: " + e.getMessage()));
    }

    public void listenUserData(String uid, UserDataCallback callback) {
        firestore.collection("users").document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        UserModel user = snapshot.toObject(UserModel.class);
                        callback.onSuccess(user);
                    }
                });
    }

    public void updateBudget(String uid, double budgetAmount, AuthCallback callback) {
        firestore.collection("users").document(uid)
                .update("monthlyBudget", budgetAmount)
                .addOnSuccessListener(aVoid -> callback.onSuccess("Cập nhật hạn mức chi tiêu thành công!"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void addTransaction(TransactionModel transaction, AuthCallback callback) {
        WriteBatch batch = firestore.batch();
        DocumentReference transRef = firestore.collection("transactions").document();
        transaction.setTransactionId(transRef.getId());
        batch.set(transRef, transaction);

        DocumentReference userRef = firestore.collection("users").document(transaction.getUserId());
        double adjustment = transaction.getType().equals("INCOME") ? transaction.getAmount() : -transaction.getAmount();
        batch.update(userRef, "totalBalance", FieldValue.increment(adjustment));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Ghi nhận giao dịch thành công!"))
                .addOnFailureListener(e -> callback.onFailure("Lỗi lưu trữ dữ liệu: " + e.getMessage()));
    }

    // ĐÃ SỬA: Tối ưu vòng lặp ép ID giao dịch thủ công để RecyclerView hiển thị không sót dữ liệu
    public void listenToTransactions(String userId, TransactionCallback callback) {
        firestore.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshot != null) {
                        List<TransactionModel> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            TransactionModel item = doc.toObject(TransactionModel.class);
                            item.setTransactionId(doc.getId()); // Đảm bảo gán chặt ID để xử lý xóa/sửa không lỗi
                            transactions.add(item);
                        }
                        callback.onUpdate(transactions);
                    }
                });
    }

    public void deleteTransaction(TransactionModel transaction, AuthCallback callback) {
        WriteBatch batch = firestore.batch();
        DocumentReference transRef = firestore.collection("transactions").document(transaction.getTransactionId());
        batch.delete(transRef);

        DocumentReference userRef = firestore.collection("users").document(transaction.getUserId());
        double reverseAdjustment = transaction.getType().equals("INCOME") ? -transaction.getAmount() : transaction.getAmount();
        batch.update(userRef, "totalBalance", FieldValue.increment(reverseAdjustment));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess("Đã xóa giao dịch!"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}