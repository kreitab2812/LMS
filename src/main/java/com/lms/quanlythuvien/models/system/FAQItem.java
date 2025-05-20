package com.lms.quanlythuvien.models.system;

public class FAQItem {
    private String question;
    private String answer;

    public FAQItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() { // <<--- Getter cho câu hỏi
        return question;
    }

    public String getAnswer() { // <<--- Getter cho câu trả lời
        return answer;
    }

    @Override
    public String toString() {
        return question; // Hữu ích nếu dùng FAQItem trực tiếp trong ListView đơn giản
    }
}