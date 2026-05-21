package tna.cntt2.appquanlychitieu;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<TransactionModel> list = new ArrayList<>();
    private final OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onDeleteClick(TransactionModel transaction);
    }

    public TransactionAdapter(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    // ĐÃ SỬA: Thay thế hoàn toàn hàm setData cũ sang cơ chế clear và update data thời gian thực
    public void updateData(List<TransactionModel> newList) {
        if (newList != null) {
            this.list.clear();              // Xóa sạch danh sách cũ đang lưu trên giao diện máy ảo
            this.list.addAll(newList);      // Nạp danh sách mới tinh vừa tải real-time từ Firestore về
            notifyDataSetChanged();         // Lệnh ép RecyclerView phải vẽ lại toàn bộ các dòng tức thì
        }
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionModel transaction = list.get(position);
        holder.tvCategory.setText(transaction.getCategory());

        // Định dạng ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = (transaction.getTimestamp() != null) ? sdf.format(transaction.getTimestamp().toDate()) : "";

        // TỐI ƯU: Kiểm tra xem ghi chú có trống hay không để hiển thị chuỗi đẹp mắt nhất
        String note = transaction.getNote() != null ? transaction.getNote().trim() : "";
        if (note.isEmpty()) {
            holder.tvNoteAndDate.setText("(" + dateStr + ")");
        } else {
            holder.tvNoteAndDate.setText(note + " (" + dateStr + ")");
        }

        // Định dạng số tiền và màu sắc dựa trên Phân loại Thu (INCOME) / Chi (EXPENSE)
        if ("INCOME".equals(transaction.getType())) {
            holder.tvAmount.setText(String.format("+%,.0f đ", transaction.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh lá cho khoản thu
        } else {
            holder.tvAmount.setText(String.format("-%,.0f đ", transaction.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#F44336")); // Màu đỏ cho khoản chi
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(transaction);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvNoteAndDate, tvAmount;
        ImageButton btnDelete;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvNoteAndDate = itemView.findViewById(R.id.tvItemNoteAndDate);
            tvAmount = itemView.findViewById(R.id.tvItemAmount);
            btnDelete = itemView.findViewById(R.id.btnDeleteTransaction);
        }
    }
}