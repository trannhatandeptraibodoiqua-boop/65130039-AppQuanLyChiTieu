package tna.cntt2.appquanlychitieu;

public class UserModel {
    private String uid;
    private String email;
    private double totalBalance;
    private double monthlyBudget;

    public UserModel() {}

    public UserModel(String uid, String email, double totalBalance, double monthlyBudget) {
        this.uid = uid;
        this.email = email;
        this.totalBalance = totalBalance;
        this.monthlyBudget = monthlyBudget;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getTotalBalance() { return totalBalance; }
    public void setTotalBalance(double totalBalance) { this.totalBalance = totalBalance; }

    public double getMonthlyBudget() { return monthlyBudget; }
    public void setMonthlyBudget(double monthlyBudget) { this.monthlyBudget = monthlyBudget; }
}