package com.lms.quanlythuvien.models.transaction; // Hoặc package phù hợp của cậu

public enum LoanStatus {
    ACTIVE,     // Sách đang được mượn và chưa đến hạn
    RETURNED,   // Sách đã được trả
    OVERDUE     // Sách đang được mượn nhưng đã quá hạn trả
    // Cậu có thể thêm các trạng thái khác nếu cần
}