package com.lms.quanlythuvien.models.transaction; // Hoặc package phù hợp của bạn

public enum LoanStatus {
    ACTIVE("Đang mượn"),        // Sách đang được mượn và chưa đến hạn
    RETURNED("Đã trả"),         // Sách đã được trả
    OVERDUE("Quá hạn");         // Sách đang được mượn nhưng đã quá hạn trả
    // Bạn có thể thêm các trạng thái khác nếu cần (ví dụ: LOST("Bị mất"))

    private final String displayName;

    LoanStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Override toString() nếu bạn muốn nó cũng trả về displayName khi gọi ngầm,
    // ví dụ khi tự động convert Enum sang String.
    @Override
    public String toString() {
        return this.displayName;
    }
}