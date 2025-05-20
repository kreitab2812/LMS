package com.lms.quanlythuvien.services.library;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lms.quanlythuvien.models.item.Book; // Model Book của bạn

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
// Không cần StreamSupport và Collectors nếu dùng vòng lặp for-each truyền thống cho JsonArray

public class GoogleBooksService {

    private static final String API_BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    // CẢNH BÁO BẢO MẬT: API Key KHÔNG NÊN hardcode.
    // Hãy đọc từ biến môi trường hoặc file cấu hình không commit lên Git.
    // Ví dụ: private static final String GOOGLE_API_KEY = System.getenv("GOOGLE_BOOKS_API_KEY");
    private static final String GOOGLE_API_KEY = "AIzaSyDQvP_9JJgL5ge8dFPQB0d2ZbRm6KOpsps"; // GIỮ LẠI THEO YÊU CẦU CỦA CẬU CHO PROJECT NÀY

    private final HttpClient httpClient;
    private final Gson gson;
    private final boolean isApiKeyAvailable;

    public GoogleBooksService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new Gson();

        // Kiểm tra API key (dù cậu muốn giữ lại, việc kiểm tra vẫn hữu ích)
        if (GOOGLE_API_KEY == null || GOOGLE_API_KEY.trim().isEmpty() ||
                GOOGLE_API_KEY.equals("YOUR_API_KEY_PLACEHOLDER") || // Thay bằng placeholder thực tế nếu có
                GOOGLE_API_KEY.equals("AIzaSyDQvP_9JJgL5ge8dFPQB0d2ZbRm6KOpsps") && !System.getProperty("ignore.placeholder.key.warning", "false").equals("true")) { // Giả sử key này là placeholder và cậu không muốn cảnh báo khi test
            System.err.println("WARN_GBS: Google Books API Key might be a placeholder or not optimally configured. " +
                    "For production, ensure a secure and unique API key is used and loaded from a config/env.");
            // Nếu là key thật của cậu thì có thể bỏ qua warning này.
            // this.isApiKeyAvailable = false; // Nếu key này chắc chắn là placeholder
            this.isApiKeyAvailable = true; // Tạm thời cho là key này dùng được theo ý cậu
        } else {
            this.isApiKeyAvailable = true;
        }
        if (this.isApiKeyAvailable) {
            System.out.println("INFO_GBS: GoogleBooksService initialized. API Key is available.");
        } else {
            System.err.println("ERROR_GBS: GoogleBooksService initialized. API Key is MISSING or INVALID. Service will not function.");
        }
    }

    private boolean ensureApiKey() {
        if (!isApiKeyAvailable) {
            System.err.println("ERROR_GBS: Operation cancelled. Google Books API Key is not available or invalid.");
            return false;
        }
        return true;
    }

    public List<Book> searchBooks(String query, int maxResults) {
        if (!ensureApiKey()) return Collections.emptyList();
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        int actualMaxResults = Math.max(1, Math.min(maxResults, 40)); // Google API giới hạn maxResults

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // Cân nhắc dùng &fields= để chỉ lấy những trường cần thiết, giảm tải dữ liệu
            // Ví dụ: &fields=items(id,volumeInfo(title,authors,publisher,publishedDate,description,industryIdentifiers,imageLinks,pageCount,averageRating,ratingsCount,categories,infoLink))
            String requestUrl = API_BASE_URL + "?q=" + encodedQuery +
                    "&maxResults=" + actualMaxResults +
                    "&printType=books" +
                    "&key=" + GOOGLE_API_KEY;

            System.out.println("DEBUG_GBS_SEARCH: Requesting Google Books API: " + requestUrl);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseBooksResponse(response.body());
            } else {
                System.err.println("ERROR_GBS_SEARCH: API Error (" + response.statusCode() + "): " + response.body());
                return Collections.emptyList();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR_GBS_SEARCH: Connection/Request Error: " + e.getMessage());
            // e.printStackTrace(); // Bật nếu cần debug sâu
            return Collections.emptyList();
        }
    }

    public Optional<Book> getBookDetailsByISBN(String isbn) {
        if (!ensureApiKey()) return Optional.empty();
        if (isbn == null || isbn.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            String encodedIsbn = URLEncoder.encode(isbn.trim(), StandardCharsets.UTF_8);
            String requestUrl = API_BASE_URL + "?q=isbn:" + encodedIsbn + "&key=" + GOOGLE_API_KEY;

            System.out.println("DEBUG_GBS_ISBN: Requesting Google Books API: " + requestUrl);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<Book> books = parseBooksResponse(response.body());
                return books.isEmpty() ? Optional.empty() : Optional.of(books.get(0));
            } else {
                System.err.println("ERROR_GBS_ISBN: API Error (" + response.statusCode() + "): " + response.body());
                return Optional.empty();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR_GBS_ISBN: Connection/Request Error: " + e.getMessage());
            // e.printStackTrace();
            return Optional.empty();
        }
    }

    private List<Book> parseBooksResponse(String jsonResponse) {
        List<Book> books = new ArrayList<>();
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) return books;

        try {
            JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);
            if (responseObject == null || !responseObject.has("items") || !responseObject.get("items").isJsonArray()) {
                if (responseObject != null && responseObject.has("error")) {
                    System.err.println("ERROR_GBS_PARSE: API returned an error: " + responseObject.getAsJsonObject("error"));
                } else {
                    System.out.println("INFO_GBS_PARSE: No 'items' array found in Google Books response or response is empty.");
                }
                return books;
            }

            JsonArray items = responseObject.getAsJsonArray("items");
            for (JsonElement itemElement : items) {
                if (!itemElement.isJsonObject()) continue;
                JsonObject item = itemElement.getAsJsonObject();

                if (!item.has("volumeInfo") || !item.get("volumeInfo").isJsonObject()) continue;
                JsonObject volumeInfo = item.getAsJsonObject("volumeInfo");

                String googleBookApiId = getJsonString(item, "id"); // ID từ Google API, dùng làm fallback

                String title = getJsonString(volumeInfo, "title", "N/A"); // Mặc định "N/A" nếu null
                List<String> authors = parseJsonStringArray(volumeInfo, "authors");
                List<String> categories = parseJsonStringArray(volumeInfo, "categories");

                String publisher = getJsonString(volumeInfo, "publisher");
                String publishedDate = getJsonString(volumeInfo, "publishedDate");
                String description = getJsonString(volumeInfo, "description");
                String infoLink = getJsonString(volumeInfo, "infoLink");

                String thumbnailUrl = null;
                if (volumeInfo.has("imageLinks") && volumeInfo.get("imageLinks").isJsonObject()) {
                    JsonObject imageLinks = volumeInfo.getAsJsonObject("imageLinks");
                    thumbnailUrl = getJsonString(imageLinks, "thumbnail");
                    if (thumbnailUrl == null) thumbnailUrl = getJsonString(imageLinks, "smallThumbnail");
                }

                String isbn10 = null;
                String isbn13 = null;
                if (volumeInfo.has("industryIdentifiers") && volumeInfo.get("industryIdentifiers").isJsonArray()) {
                    JsonArray identifiers = volumeInfo.getAsJsonArray("industryIdentifiers");
                    for (JsonElement idElement : identifiers) {
                        if (!idElement.isJsonObject()) continue;
                        JsonObject idObj = idElement.getAsJsonObject();
                        String type = getJsonString(idObj, "type");
                        String identifierValue = getJsonString(idObj, "identifier");
                        if ("ISBN_10".equals(type)) isbn10 = identifierValue;
                        else if ("ISBN_13".equals(type)) isbn13 = identifierValue;
                    }
                }

                Integer pageCount = getJsonInt(volumeInfo, "pageCount");
                Double averageRating = getJsonDouble(volumeInfo, "averageRating");
                Integer ratingsCount = getJsonInt(volumeInfo, "ratingsCount");

                // Ưu tiên ISBN-13 làm ID chính cho sách. Nếu không có, dùng Google Book API ID.
                String primaryIdForBook = isbn13 != null ? isbn13 : googleBookApiId;
                if (primaryIdForBook == null) {
                    System.err.println("WARN_GBS_PARSE: Book item found without ISBN-13 or Google API ID. Title: " + title + ". Skipping.");
                    continue; // Bỏ qua sách này nếu không có ID khả dĩ
                }

                // Constructor Book:
                // String isbn13_as_id, String title, List<String> authors, String publisher, String publishedDate,
                // String description, List<String> categories, String thumbnailUrl, String infoLink,
                // String isbn10, Integer pageCount, Double averageRating, Integer ratingsCount, int initialQuantity
                Book book = new Book(
                        primaryIdForBook,
                        title,
                        authors,
                        publisher,
                        publishedDate,
                        description,
                        categories,
                        thumbnailUrl,
                        infoLink,
                        isbn10,
                        pageCount,
                        averageRating,
                        ratingsCount,
                        0 // initialQuantity khi lấy từ Google Books là 0, admin sẽ cập nhật sau
                );
                books.add(book);
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            System.err.println("ERROR_GBS_PARSE: JSON Syntax Error parsing Google Books response: " + e.getMessage());
            // e.printStackTrace();
        } catch (Exception e) {
            System.err.println("ERROR_GBS_PARSE: Unexpected error parsing Google Books response: " + e.getMessage());
            // e.printStackTrace();
        }
        return books;
    }

    // Helper để parse JsonObject lấy String, có giá trị mặc định
    private String getJsonString(JsonObject obj, String key, String defaultValue) {
        JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull() && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return defaultValue;
    }

    // Helper để parse JsonObject lấy String (trả về null nếu không có hoặc không phải string)
    private String getJsonString(JsonObject obj, String key) {
        return getJsonString(obj, key, null);
    }

    // Helper để parse JsonObject lấy Integer
    private Integer getJsonInt(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull() && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt();
        }
        return null;
    }

    // Helper để parse JsonObject lấy Double
    private Double getJsonDouble(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull() && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsDouble();
        }
        return null;
    }

    // Helper để parse JsonArray thành List<String>
    private List<String> parseJsonStringArray(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key) && obj.get(key).isJsonArray()) {
            JsonArray array = obj.getAsJsonArray(key);
            for (JsonElement element : array) {
                if (element != null && !element.isJsonNull() && element.isJsonPrimitive()) {
                    list.add(element.getAsString());
                }
            }
        }
        return list;
    }

    public List<Book> getRandomRecommendedBooks(int count) {
        if (!ensureApiKey()) return Collections.emptyList();

        List<String> commonQueries = List.of(
                "vietnamese literature", "world history", "classic novels", "data science",
                "science fiction", "fantasy series", "contemporary fiction", "software architecture",
                "mystery thriller", "biography", "self-help", "technology trends", "clean code",
                "java programming", "software development", "artificial intelligence"
        );
        String randomQuery = commonQueries.get((int) (Math.random() * commonQueries.size()));
        System.out.println("DEBUG_GBS_RECOMMEND: Fetching recommendations for query: " + randomQuery);
        int actualCount = Math.max(1, Math.min(count, 10)); // Giới hạn số lượng sách gợi ý
        return searchBooks(randomQuery, actualCount);
    }
}