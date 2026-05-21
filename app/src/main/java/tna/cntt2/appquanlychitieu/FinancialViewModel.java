package tna.cntt2.appquanlychitieu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

public class FinancialViewModel extends ViewModel {
    private final FinancialRepository repository = new FinancialRepository();

    private final MutableLiveData<UserModel> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<TransactionModel>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();

    public LiveData<UserModel> getUserLiveData() { return userLiveData; }
    public LiveData<List<TransactionModel>> getTransactionsLiveData() { return transactionsLiveData; }
    public LiveData<String> getStatusMessage() { return statusMessage; }

    public void register(String email, String password) {
        repository.registerUser(email, password, new AuthCallback() {
            @Override
            public void onSuccess(String msg) { statusMessage.setValue(msg); }
            @Override
            public void onFailure(String errorMsg) { statusMessage.setValue(errorMsg); }
        });
    }

    public void login(String email, String password) {
        repository.loginUser(email, password, new AuthCallback() {
            @Override
            public void onSuccess(String msg) { statusMessage.setValue(msg); }
            @Override
            public void onFailure(String errorMsg) { statusMessage.setValue(errorMsg); }
        });
    }

    // ĐÃ SỬA: Chuyển đổi sang .setValue() để ép luồng Main Thread cập nhật UI tức thì
    public void initUserData(String uid) {
        repository.listenUserData(uid, new UserDataCallback() {
            @Override
            public void onSuccess(UserModel user) {
                userLiveData.setValue(user); // Cập nhật số dư tổng lên giao diện ngay lập tức
            }
            @Override
            public void onFailure(String error) {
                statusMessage.setValue(error);
            }
        });

        repository.listenToTransactions(uid, new TransactionCallback() {
            @Override
            public void onUpdate(List<TransactionModel> transactions) {
                transactionsLiveData.setValue(transactions); // Ép RecyclerView nạp dữ liệu mới tức thì
            }
            @Override
            public void onError(String error) {
                statusMessage.setValue(error);
            }
        });
    }

    public void addTransaction(TransactionModel transaction) {
        repository.addTransaction(transaction, new AuthCallback() {
            @Override
            public void onSuccess(String msg) { statusMessage.setValue(msg); }
            @Override
            public void onFailure(String errorMsg) { statusMessage.setValue(errorMsg); }
        });
    }

    public void setMonthlyBudget(String uid, double amount) {
        repository.updateBudget(uid, amount, new AuthCallback() {
            @Override
            public void onSuccess(String msg) { statusMessage.setValue(msg); }
            @Override
            public void onFailure(String errorMsg) { statusMessage.setValue(errorMsg); }
        });
    }

    public void removeTransaction(TransactionModel transaction) {
        repository.deleteTransaction(transaction, new AuthCallback() {
            @Override
            public void onSuccess(String msg) { statusMessage.setValue(msg); }
            @Override
            public void onFailure(String errorMsg) { statusMessage.setValue(errorMsg); }
        });
    }
}