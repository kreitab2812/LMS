package com.lms.quanlythuvien.services;

import com.lms.quanlythuvien.models.Book; // Đảm bảo import Book model của cậu
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// import java.util.UUID; // Có thể không cần nếu ID sách chủ yếu là ISBN
import java.util.stream.Collectors;

public class BookManagementService {

    private final List<Book> libraryBooks;

    // Constructor có thể là public nếu các controller cần tạo instance mới
    // Hoặc private nếu cậu dùng Singleton pattern cho service này (như đã thảo luận)
    public BookManagementService() {
        this.libraryBooks = new ArrayList<>();
        initializeSampleBooks(); // Bỏ comment nếu muốn có sách mẫu khi khởi tạo
        System.out.println("DEBUG_BMS: BookManagementService initialized. Library size: " + this.libraryBooks.size());
    }

    // (Tùy chọn) Phương thức để thêm sách mẫu ban đầu
    private void initializeSampleBooks() {
        System.out.println("DEBUG_BMS_INIT: Initializing sample books...");
        // Sách 1
        // Sử dụng constructor của Book đã được cập nhật để nhận initialQuantity
        // Book(id, title, authors, publisher, publishedDate, description, categories,
        //      thumbnailUrl, infoLink, isbn10, isbn13, pageCount,
        //      averageRating, ratingsCount, initialQuantity)
        Book sampleBook1 = new Book(
                "978-0132350884",
                "Clean Code: A Handbook of Agile Software Craftsmanship",
                List.of("Robert C. Martin"),
                "Prentice Hall",
                "2008-08-01",
                "Even bad code can function. But if code isn't clean...",
                List.of("Software Engineering", "Programming"),
                "http://books.google.com/...", // thumbnailUrl
                "http://books.google.com/...", // infoLink
                "0132350882",
                "978-0132350884",
                464,
                4.5,
                1234, // ratingsCount
                10    // initialQuantity (sẽ tự set totalQuantity = 10, availableQuantity = 10)
        );
        sampleBook1.setShelfLocation("Kệ A1-01");
        // Kiểm tra để tránh thêm trùng lặp nếu initializeSampleBooks được gọi nhiều lần (ví dụ nếu service không phải Singleton)
        if (this.libraryBooks.stream().noneMatch(b -> b.getId().equals(sampleBook1.getId()))) {
            this.libraryBooks.add(sampleBook1);
        }


        Book sampleBook2 = new Book(
                "978-0321765723",
                "Effective Java",
                List.of("Joshua Bloch"),
                "Addison-Wesley Professional",
                "2018-01-06",
                "The Definitive Guide to Java Platform Best Practices...",
                List.of("Java", "Programming"),
                "http://books.google.com/...", // thumbnailUrl
                "http://books.google.com/...", // infoLink
                "0321765723",
                "978-0321765723",
                412,
                4.7,
                5678, // ratingsCount
                5     // initialQuantity
        );
        sampleBook2.setShelfLocation("Kệ A1-02");
        if (this.libraryBooks.stream().noneMatch(b -> b.getId().equals(sampleBook2.getId()))) {
            this.libraryBooks.add(sampleBook2);
        }

        System.out.println("DEBUG_BMS_INIT: Sample books initialization finished. Current library size: " + this.libraryBooks.size());
    }


    public boolean addBookToLibrary(Book newBook) {
        if (newBook == null || newBook.getId() == null || newBook.getId().trim().isEmpty()) {
            System.err.println("ERROR_BMS_ADD: Book or Book ID cannot be null or empty.");
            return false;
        }
        // Constructor của Book (phiên bản mới) đã tự xử lý việc gán totalQuantity và availableQuantity
        // dựa trên initialQuantity được truyền vào.
        if (findBookByIdInLibrary(newBook.getId()).isPresent()) {
            System.err.println("ERROR_BMS_ADD: Book with ID " + newBook.getId() + " already exists.");
            return false;
        }
        libraryBooks.add(newBook);
        System.out.println("DEBUG_BMS_ADD: Book added to library: '" + newBook.getTitle() +
                "'. TotalQ: " + newBook.getTotalQuantity() +
                ", AvailQ: " + newBook.getAvailableQuantity() +
                ". Total books in lib: " + libraryBooks.size());
        return true;
    }

    public boolean updateBookInLibrary(Book updatedBookFromUI) {
        if (updatedBookFromUI == null || updatedBookFromUI.getId() == null) {
            System.err.println("ERROR_BMS_UPDATE: Updated book or its ID cannot be null.");
            return false;
        }
        Optional<Book> existingBookOpt = findBookByIdInLibrary(updatedBookFromUI.getId());
        if (existingBookOpt.isPresent()) {
            Book existingBookInList = existingBookOpt.get();

            // Cập nhật các thông tin mô tả
            existingBookInList.setTitle(updatedBookFromUI.getTitle());
            existingBookInList.setAuthors(updatedBookFromUI.getAuthors());
            existingBookInList.setPublisher(updatedBookFromUI.getPublisher());
            existingBookInList.setPublishedDate(updatedBookFromUI.getPublishedDate());
            existingBookInList.setDescription(updatedBookFromUI.getDescription());
            existingBookInList.setCategories(updatedBookFromUI.getCategories());
            existingBookInList.setThumbnailUrl(updatedBookFromUI.getThumbnailUrl());
            existingBookInList.setInfoLink(updatedBookFromUI.getInfoLink()); // Giả sử Book có getInfoLink()
            existingBookInList.setIsbn10(updatedBookFromUI.getIsbn10());
            existingBookInList.setIsbn13(updatedBookFromUI.getIsbn13());
            existingBookInList.setPageCount(updatedBookFromUI.getPageCount());
            existingBookInList.setShelfLocation(updatedBookFromUI.getShelfLocation());
            // Cập nhật cả thông tin rating nếu có trong updatedBookFromUI và model Book hỗ trợ
            existingBookInList.setAverageRating(updatedBookFromUI.getAverageRating());
            existingBookInList.setRatingsCount(updatedBookFromUI.getRatingsCount());


            // Xử lý cập nhật totalQuantity và availableQuantity một cách cẩn thận
            int oldTotalQuantity = existingBookInList.getTotalQuantity();
            int oldAvailableQuantity = existingBookInList.getAvailableQuantity();
            int newTotalQuantityFromDialog = updatedBookFromUI.getTotalQuantity(); // Đây là total quantity mới từ dialog

            // Số sách đang được mượn (ngầm định từ số lượng cũ)
            int currentlyBorrowedCount = oldTotalQuantity - oldAvailableQuantity;
            if (currentlyBorrowedCount < 0) { // Đảm bảo currentlyBorrowedCount không âm (trường hợp dữ liệu không nhất quán)
                System.err.println("WARN_BMS_UPDATE: Inconsistent old quantities for book " + existingBookInList.getId() + ". Resetting borrowed count to 0 for calculation.");
                currentlyBorrowedCount = 0;
            }

            // Cập nhật totalQuantity cho sách hiện có trong danh sách
            existingBookInList.setTotalQuantity(newTotalQuantityFromDialog);

            // Tính toán availableQuantity mới
            // availableQuantity mới = totalQuantity mới - số sách đang mượn
            int newAvailableQuantity = newTotalQuantityFromDialog - currentlyBorrowedCount;

            // Đảm bảo availableQuantity mới không âm và không lớn hơn totalQuantity mới
            if (newAvailableQuantity < 0) {
                System.err.println("WARN_BMS_UPDATE: New total quantity " + newTotalQuantityFromDialog +
                        " for book ID " + existingBookInList.getId() +
                        " is less than the number of implied currently borrowed books (" + currentlyBorrowedCount +
                        "). This might indicate an issue or require admin action on loans.");
                // Trong trường hợp này, availableQuantity sẽ là 0,
                // và totalQuantity có thể cần được điều chỉnh lại bằng currentlyBorrowedCount nếu không cho phép total < borrowed.
                // Hoặc, đây là một lỗi và không nên cho phép cập nhật nếu total mới < số đang mượn.
                // Tạm thời, chúng ta sẽ giới hạn availableQuantity = 0 nếu nó âm.
                newAvailableQuantity = 0;
                // Nếu total mới nhỏ hơn số đang mượn, có thể nên báo lỗi rõ ràng hơn hoặc không cho phép.
                // For now, we'll set available to 0 and keep the new total.
            }
            if (newAvailableQuantity > newTotalQuantityFromDialog) { // Không thể xảy ra nếu currentlyBorrowedCount >= 0
                newAvailableQuantity = newTotalQuantityFromDialog;
            }

            existingBookInList.setAvailableQuantity(newAvailableQuantity);

            System.out.println("DEBUG_BMS_UPDATE: Book updated: " + existingBookInList.getTitle() +
                    ". TotalQ: " + existingBookInList.getTotalQuantity() +
                    ", AvailQ: " + existingBookInList.getAvailableQuantity());
            return true;
        }
        System.err.println("ERROR_BMS_UPDATE: Book with ID " + updatedBookFromUI.getId() + " not found for update.");
        return false;
    }

    public boolean deleteBookFromLibrary(String bookId) {
        if (bookId == null) {
            System.err.println("ERROR_BMS_DELETE: Book ID cannot be null.");
            return false;
        }
        // Cần kiểm tra xem sách có đang được ai đó mượn không (thông qua BorrowingRecordService)
        // Tạm thời, chúng ta vẫn xóa trực tiếp. Nếu đã làm Singleton:
        // BorrowingRecordService brs = BorrowingRecordService.getInstance();
        // if (!brs.getActiveLoansByBookId(bookId).isEmpty()) {
        //     System.err.println("ERROR_BMS_DELETE: Cannot delete book " + bookId + ". It has active loans.");
        //     return false;
        // }

        boolean removed = libraryBooks.removeIf(book -> bookId.equals(book.getId()));
        if (removed) {
            System.out.println("DEBUG_BMS_DELETE: Book deleted with ID: " + bookId + ". Total books: " + libraryBooks.size());
        } else {
            System.err.println("ERROR_BMS_DELETE: Book with ID '" + bookId + "' not found for deletion.");
        }
        return removed;
    }

    public Optional<Book> findBookByIdInLibrary(String bookId) {
        if (bookId == null) return Optional.empty();
        return libraryBooks.stream()
                .filter(book -> bookId.equals(book.getId()))
                .findFirst();
    }

    public Optional<Book> findBookByIsbn13InLibrary(String isbn13) {
        if (isbn13 == null || isbn13.trim().isEmpty()) return Optional.empty();
        return libraryBooks.stream()
                .filter(book -> book.getIsbn13() != null && isbn13.equals(book.getIsbn13()))
                .findFirst();
    }

    public List<Book> getAllBooksInLibrary() {
        return new ArrayList<>(libraryBooks);
    }

    public List<Book> searchBooksInLibrary(String keyword, String searchType) {
        // Giữ nguyên logic của cậu
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooksInLibrary();
        }
        String lowerCaseKeyword = keyword.toLowerCase();
        return libraryBooks.stream()
                .filter(book -> {
                    // ... (logic tìm kiếm của cậu) ...
                    if (searchType == null || searchType.equalsIgnoreCase("ALL")) {
                        return (book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseKeyword)) ||
                                (book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(lowerCaseKeyword))) ||
                                (book.getIsbn10() != null && book.getIsbn10().contains(lowerCaseKeyword)) ||
                                (book.getIsbn13() != null && book.getIsbn13().contains(lowerCaseKeyword)) ||
                                (book.getPublisher() != null && book.getPublisher().toLowerCase().contains(lowerCaseKeyword));
                    } else if (searchType.equalsIgnoreCase("TITLE")) {
                        return book.getTitle() != null && book.getTitle().toLowerCase().contains(lowerCaseKeyword);
                    } else if (searchType.equalsIgnoreCase("AUTHOR")) {
                        return book.getAuthors() != null && book.getAuthors().stream().anyMatch(author -> author.toLowerCase().contains(lowerCaseKeyword));
                    } else if (searchType.equalsIgnoreCase("ISBN")) {
                        return (book.getIsbn10() != null && book.getIsbn10().equals(lowerCaseKeyword)) ||
                                (book.getIsbn13() != null && book.getIsbn13().equals(lowerCaseKeyword));
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    // Các phương thức này được gọi bởi LoanManagementController (hoặc service khác)
    // để cập nhật trạng thái sẵn có của sách.
    public boolean handleBookBorrowed(String bookId) {
        Optional<Book> bookOpt = findBookByIdInLibrary(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            // Phương thức borrowBook() trong Book model sẽ tự giảm availableQuantity
            if (book.borrowBook()) { // borrowBook() nên trả về true nếu thành công
                System.out.println("DEBUG_BMS_BORROW: Book ID " + bookId + " processed for borrowing. Available now: " + book.getAvailableQuantity());
                return true;
            } else {
                System.err.println("ERROR_BMS_BORROW: Book ID " + bookId + " is not available (borrowBook() in model failed).");
                return false;
            }
        }
        System.err.println("ERROR_BMS_BORROW: Book ID " + bookId + " not found for borrowing.");
        return false;
    }

    public boolean handleBookReturned(String bookId) {
        Optional<Book> bookOpt = findBookByIdInLibrary(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            // Phương thức returnBook() trong Book model sẽ tự tăng availableQuantity
            book.returnBook();
            System.out.println("DEBUG_BMS_RETURN: Book ID " + bookId + " processed for return. Available now: " + book.getAvailableQuantity());
            return true;
        }
        System.err.println("ERROR_BMS_RETURN: Book ID " + bookId + " not found for returning.");
        return false;
    }
}