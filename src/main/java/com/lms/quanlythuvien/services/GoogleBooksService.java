package com.lms.quanlythuvien.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lms.quanlythuvien.models.Book;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GoogleBooksService {

    private static final String API_BASE_URL = "https://www.googleapis.com/books/v1/volumes";
    // QUAN TRỌNG: Thay thế giá trị này bằng API Key MỚI và AN TOÀN của bạn.
    // KHÔNG commit API Key thực tế vào kho mã nguồn công khai.
    // Cách tốt hơn là đọc từ biến môi trường hoặc tệp cấu hình.
    private static final String GOOGLE_API_KEY = "AIzaSyDQvP_9JJgL5ge8dFPQB0d2ZbRm6KOpsps"; // <<<=== API KEY CỦA BẠN (HÃY CẨN THẬN BẢO MẬT NÓ)

    private final HttpClient httpClient;
    private final Gson gson;

    public GoogleBooksService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();

        // Bạn có thể giữ lại hoặc bỏ đi phần kiểm tra key placeholder này nếu muốn
        if ("YOUR_NEW_SECURE_API_KEY".equals(GOOGLE_API_KEY) || "AIzaSyDQvP_9JJgL5ge8dFPQB0d2ZbRm6KOpsps_PLACEHOLDER".equals(GOOGLE_API_KEY) || GOOGLE_API_KEY == null || GOOGLE_API_KEY.trim().isEmpty()) {
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("!!! QUAN TRỌNG: Vui lòng kiểm tra lại GOOGLE_API_KEY              !!!");
            System.err.println("!!! trong GoogleBooksService.java. Đảm bảo đó là API Key hợp lệ.  !!!");
            System.err.println("!!! Nếu không, các yêu cầu đến Google Books API sẽ thất bại.        !!!");
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    public List<Book> searchBooks(String query, int maxResults) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (GOOGLE_API_KEY == null || GOOGLE_API_KEY.trim().isEmpty() || GOOGLE_API_KEY.startsWith("YOUR_")) { // Kiểm tra kỹ hơn
            System.err.println("Google Books API Key is not configured or is a placeholder. Cannot perform search.");
            return Collections.emptyList();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String requestUrl = API_BASE_URL + "?q=" + encodedQuery +
                    "&maxResults=" + maxResults +
                    "&printType=books" +
                    "&key=" + GOOGLE_API_KEY;

            System.out.println("Requesting Google Books API (searchBooks): " + requestUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseBooksResponse(response.body());
            } else {
                System.err.println("Error from Google Books API (searchBooks): " + response.statusCode() + " - " + response.body());
                return Collections.emptyList();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error connecting to Google Books API (searchBooks): " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Optional<Book> getBookDetailsByISBN(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return Optional.empty();
        }
        if (GOOGLE_API_KEY == null || GOOGLE_API_KEY.trim().isEmpty() || GOOGLE_API_KEY.startsWith("YOUR_")) {
            System.err.println("Google Books API Key is not configured or is a placeholder. Cannot fetch by ISBN.");
            return Optional.empty();
        }

        try {
            String encodedIsbn = URLEncoder.encode(isbn, StandardCharsets.UTF_8);
            String requestUrl = API_BASE_URL + "?q=isbn:" + encodedIsbn + "&key=" + GOOGLE_API_KEY;

            System.out.println("Requesting Google Books API (getBookDetailsByISBN): " + requestUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Book> books = parseBooksResponse(response.body());
                if (!books.isEmpty()) {
                    return Optional.of(books.get(0));
                } else {
                    System.out.println("No book found on Google Books for ISBN: " + isbn);
                    return Optional.empty();
                }
            } else {
                System.err.println("Error from Google Books API (getBookDetailsByISBN): " + response.statusCode() + " - " + response.body());
                return Optional.empty();
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error connecting to Google Books API (getBookDetailsByISBN): " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private List<Book> parseBooksResponse(String jsonResponse) {
        List<Book> books = new ArrayList<>();
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return books;
        }
        JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);

        if (responseObject.has("items") && responseObject.get("items").isJsonArray()) {
            JsonArray items = responseObject.getAsJsonArray("items");
            for (JsonElement itemElement : items) {
                if (!itemElement.isJsonObject()) continue;
                JsonObject item = itemElement.getAsJsonObject();

                // DÒNG ĐÃ ĐƯỢC SỬA CHÍNH XÁC:
                if (!item.has("volumeInfo") || !item.get("volumeInfo").isJsonObject()) continue;

                JsonObject volumeInfo = item.getAsJsonObject("volumeInfo");

                String id = item.has("id") ? item.get("id").getAsString() : null;
                String title = volumeInfo.has("title") ? volumeInfo.get("title").getAsString() : "N/A";

                List<String> authors = new ArrayList<>();
                if (volumeInfo.has("authors") && volumeInfo.get("authors").isJsonArray()) {
                    JsonArray authorsArray = volumeInfo.getAsJsonArray("authors");
                    authors = StreamSupport.stream(authorsArray.spliterator(), false)
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsString)
                            .collect(Collectors.toList());
                }

                String publisher = volumeInfo.has("publisher") ? volumeInfo.get("publisher").getAsString() : null;
                String publishedDate = volumeInfo.has("publishedDate") ? volumeInfo.get("publishedDate").getAsString() : null;
                String description = volumeInfo.has("description") ? volumeInfo.get("description").getAsString() : null;

                List<String> categories = new ArrayList<>();
                if (volumeInfo.has("categories") && volumeInfo.get("categories").isJsonArray()) {
                    JsonArray categoriesArray = volumeInfo.getAsJsonArray("categories");
                    categories = StreamSupport.stream(categoriesArray.spliterator(), false)
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsString)
                            .collect(Collectors.toList());
                }

                String thumbnailUrl = null;
                if (volumeInfo.has("imageLinks") && volumeInfo.get("imageLinks").isJsonObject()) {
                    JsonObject imageLinks = volumeInfo.getAsJsonObject("imageLinks");
                    if (imageLinks.has("thumbnail")) {
                        thumbnailUrl = imageLinks.get("thumbnail").getAsString();
                    } else if (imageLinks.has("smallThumbnail")) {
                        thumbnailUrl = imageLinks.get("smallThumbnail").getAsString();
                    }
                }

                String infoLink = volumeInfo.has("infoLink") ? volumeInfo.get("infoLink").getAsString() : null;

                String isbn10 = null;
                String isbn13 = null;
                if (volumeInfo.has("industryIdentifiers") && volumeInfo.get("industryIdentifiers").isJsonArray()) {
                    JsonArray identifiers = volumeInfo.getAsJsonArray("industryIdentifiers");
                    for (JsonElement idElement : identifiers) {
                        if (!idElement.isJsonObject()) continue;
                        JsonObject identifierObject = idElement.getAsJsonObject();
                        if (identifierObject.has("type") && identifierObject.has("identifier")) {
                            String type = identifierObject.get("type").getAsString();
                            String identifierValue = identifierObject.get("identifier").getAsString();
                            if ("ISBN_10".equals(type)) {
                                isbn10 = identifierValue;
                            } else if ("ISBN_13".equals(type)) {
                                isbn13 = identifierValue;
                            }
                        }
                    }
                }

                Integer pageCount = (volumeInfo.has("pageCount") && volumeInfo.get("pageCount").isJsonPrimitive())
                        ? volumeInfo.get("pageCount").getAsInt() : null;
                Double averageRating = (volumeInfo.has("averageRating") && volumeInfo.get("averageRating").isJsonPrimitive())
                        ? volumeInfo.get("averageRating").getAsDouble() : null;
                Integer ratingsCount = (volumeInfo.has("ratingsCount") && volumeInfo.get("ratingsCount").isJsonPrimitive())
                        ? volumeInfo.get("ratingsCount").getAsInt() : null;

                Book book = new Book(id, title, authors, publisher, publishedDate, description,
                        categories, thumbnailUrl, infoLink,
                        isbn10, isbn13, pageCount, averageRating, ratingsCount);
                books.add(book);
            }
        }
        return books;
    }

    public List<Book> getRandomRecommendedBooks(int count) {
        List<String> commonQueries = List.of("Java programming", "classic literature", "software architecture", "data structures algorithms", "vietnamese literature", "world history", "artificial intelligence", "clean code");
        String randomQuery = commonQueries.get((int) (Math.random() * commonQueries.size()));
        System.out.println("Fetching recommendations for query: " + randomQuery);
        // Đảm bảo count không quá lớn (Google API có giới hạn maxResults, thường là 40)
        return searchBooks(randomQuery, Math.max(1, Math.min(count, 40)));
    }
}