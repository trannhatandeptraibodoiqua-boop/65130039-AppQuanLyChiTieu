package tna.cntt2.appquanlychitieu;
import java.util.List;

interface AuthCallback {
    void onSuccess(String msg);
    void onFailure(String errorMsg);
}

interface UserDataCallback {
    void onSuccess(UserModel user);
    void onFailure(String error);
}

interface TransactionCallback {
    void onUpdate(List<TransactionModel> transactions);
    void onError(String error);
}
