package com.lms.quanlythuvien.services;

import com.lms.quanlythuvien.models.Book; // Đảm bảo import Book model của cậu
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookManagementService {

    private static BookManagementService instance; // <<<--- BIẾN STATIC CHO SINGLETON
    private final List<Book> libraryBooks;

    // <<<--- CONSTRUCTOR LÀ PRIVATE CHO SINGLETON
    private BookManagementService() {
        this.libraryBooks = new ArrayList<>();
        initializeSampleBooks(); // Khởi tạo sách mẫu
        System.out.println("DEBUG_BMS_SINGLETON: BookManagementService Singleton instance created. Initial library size: " + this.libraryBooks.size());
    }

    // <<<--- PHƯƠNG THỨC STATIC ĐỂ LẤY INSTANCE SINGLETON
    public static synchronized BookManagementService getInstance() {
        if (instance == null) {
            instance = new BookManagementService();
        }
        return instance;
    }

    private void initializeSampleBooks() {
        System.out.println("DEBUG_BMS_INIT: Initializing sample books...");

        // Sách mẫu 1
        // Sử dụng constructor của Book:
        // Book(id, title, authors, publisher, publishedDate, description, categories,
        //      thumbnailUrl, infoLink, isbn10, isbn13, pageCount,
        //      averageRating, ratingsCount, initialQuantity)
        Book sampleBook1 = new Book(
                "978-0132350884", "Clean Code: A Handbook of Agile Software Craftsmanship",
                List.of("Robert C. Martin"), "Prentice Hall", "2008-08-01",
                "Even bad code can function. But if code isn't clean...",
                List.of("Software Engineering", "Programming"),
                "http://books.google.com/...", "http://books.google.com/...",
                "0132350882", "978-0132350884", 464, 4.5, 1234,
                10 // initialQuantity: totalQuantity = 10, availableQuantity = 10
        );
        sampleBook1.setShelfLocation("Kệ A1-01");
        // Chỉ thêm nếu sách chưa có trong danh sách (quan trọng cho Singleton)
        if (this.libraryBooks.stream().noneMatch(b -> b.getId().equals(sampleBook1.getId()))) {
            this.libraryBooks.add(sampleBook1);
        }

        // Sách mẫu 2
        Book sampleBook2 = new Book(
                "978-0321765723", "Effective Java", List.of("Joshua Bloch"),
                "Addison-Wesley Professional", "2018-01-06",
                "The Definitive Guide to Java Platform Best Practices...",
                List.of("Java", "Programming"), "http://books.google.com/...",
                "http://books.google.com/...", "0321765723", "978-0321765723",
                412, 4.7, 5678,
                5 // initialQuantity: totalQuantity = 5, availableQuantity = 5
        );
        sampleBook2.setShelfLocation("Kệ A1-02");
        if (this.libraryBooks.stream().noneMatch(b -> b.getId().equals(sampleBook2.getId()))) {
            this.libraryBooks.add(sampleBook2);
        }

        // Thêm các sách mẫu khác nếu cậu muốn, ví dụ:
        Book sampleBook3 = new Book("IDSACH003", "The Pragmatic Programmer", List.of("Andrew Hunt", "David Thomas"), "Addison-Wesley", "1999-10-20", "Description...", List.of("Programming"), null, null, "ISBN10-003", "ISBN13-003", 352, 4.6, 2000, 7);
        sampleBook3.setShelfLocation("Kệ B2-05");
        if (this.libraryBooks.stream().noneMatch(b -> b.getId().equals(sampleBook3.getId()))) {
            this.libraryBooks.add(sampleBook3);
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
            existingBookInList.setInfoLink(updatedBookFromUI.getInfoLink());
            existingBookInList.setIsbn10(updatedBookFromUI.getIsbn10());
            existingBookInList.setIsbn13(updatedBookFromUI.getIsbn13());
            existingBookInList.setPageCount(updatedBookFromUI.getPageCount());
            existingBookInList.setShelfLocation(updatedBookFromUI.getShelfLocation());
            existingBookInList.setAverageRating(updatedBookFromUI.getAverageRating());
            existingBookInList.setRatingsCount(updatedBookFromUI.getRatingsCount());

            // Xử lý cập nhật totalQuantity và availableQuantity
            int oldTotalQuantity = existingBookInList.getTotalQuantity();
            int oldAvailableQuantity = existingBookInList.getAvailableQuantity();
            int newTotalQuantityFromDialog = updatedBookFromUI.getTotalQuantity(); // Giá trị này từ dialog (ý là tổng số sách admin muốn có)

            int currentlyBorrowedCount = oldTotalQuantity - oldAvailableQuantity;
            if (currentlyBorrowedCount < 0) { // Sanity check
                System.err.println("WARN_BMS_UPDATE: Inconsistent old quantities for book " + existingBookInList.getId() + ". Resetting borrowed count to 0 for calculation.");
                currentlyBorrowedCount = 0;
            }

            existingBookInList.setTotalQuantity(newTotalQuantityFromDialog); // Cập nhật tổng số lượng mới

            // Tính lại số lượng có sẵn mới
            int newAvailableQuantity = newTotalQuantityFromDialog - currentlyBorrowedCount;

            if (newAvailableQuantity < 0) {
                System.err.println("WARN_BMS_UPDATE: New total quantity (" + newTotalQuantityFromDialog +
                        ") for book ID " + existingBookInList.getId() +
                        " is less than implied borrowed books (" + currentlyBorrowedCount +
                        "). Setting available quantity to 0. Admin should review loans for this book.");
                newAvailableQuantity = 0;
                // Cân nhắc: Nếu newTotalQuantityFromDialog < currentlyBorrowedCount, có thể không cho phép cập nhật
                // hoặc tự động đặt newTotalQuantityFromDialog = currentlyBorrowedCount để đảm bảo tính nhất quán hơn.
                // Ví dụ: existingBookInList.setTotalQuantity(Math.max(newTotalQuantityFromDialog, currentlyBorrowedCount));
            }
            // Đảm bảo availableQuantity không vượt quá totalQuantity mới
            if (newAvailableQuantity > existingBookInList.getTotalQuantity()) {
                newAvailableQuantity = existingBookInList.getTotalQuantity();
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
        // Trong tương lai, khi BorrowingRecordService là Singleton và có thể truy cập được:
        // BorrowingRecordService brs = BorrowingRecordService.getInstance();
        // if (!brs.getActiveLoansByBookId(bookId).isEmpty()) {
        //     System.err.println("ERROR_BMS_DELETE: Cannot delete book " + bookId + ". It has active loans.");
        //     return false; // Hoặc ném một custom exception
        // }

        boolean removed = libraryBooks.removeIf(book -> bookId.equals(book.getId()));
        if (removed) {
            System.out.println("DEBUG_BMS_DELETE: Book deleted with ID: " + bookId + ". Total books now: " + libraryBooks.size());
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
        return new ArrayList<>(libraryBooks); // Trả về bản sao
    }

    public List<Book> searchBooksInLibrary(String keyword, String searchType) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooksInLibrary();
        }
        String lowerCaseKeyword = keyword.toLowerCase();
        return libraryBooks.stream()
                .filter(book -> {
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

    public boolean handleBookBorrowed(String bookId) {
        Optional<Book> bookOpt = findBookByIdInLibrary(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            if (book.borrowBook()) { // Giả sử book.borrowBook() trả về true nếu thành công
                System.out.println("DEBUG_BMS_BORROW: Book ID " + bookId + " processed for borrowing. Available now: " + book.getAvailableQuantity());
                return true;
            } else {
                System.err.println("ERROR_BMS_BORROW: Book ID " + bookId + " is not available (model.borrowBook() failed).");
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
            book.returnBook(); // Giả sử book.returnBook() không cần trả về boolean và luôn thành công nếu sách tồn tại
            System.out.println("DEBUG_BMS_RETURN: Book ID " + bookId + " processed for return. Available now: " + book.getAvailableQuantity());
            return true;
        }
        System.err.println("ERROR_BMS_RETURN: Book ID " + bookId + " not found for returning.");
        return false;
    }
}