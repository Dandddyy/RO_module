import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

class Library {
    private List<Book> books;
    private Semaphore readingRoomSemaphore;
    private Map<Book, Semaphore> bookSemaphores;
    private Random random = new Random();
    private Map<String, Integer> booksTakenByReader = new HashMap<>();

    public Library(List<Book> books, int maxReadersInRoom) {
        this.books = books;
        this.readingRoomSemaphore = new Semaphore(maxReadersInRoom);
        this.bookSemaphores = new HashMap<>();

        for (Book book : books) {
            bookSemaphores.put(book, new Semaphore(1));
        }
    }

    public void readerThread() {
        try {
            readingRoomSemaphore.acquire();
            String readerName = Thread.currentThread().getName();
            int maxBooksToTake = 2; // Максимальное количество книг, которые читатель может взять

            if (!booksTakenByReader.containsKey(readerName)) {
                booksTakenByReader.put(readerName, 0);
            }

            int booksTaken = booksTakenByReader.get(readerName);
            int booksRemaining = maxBooksToTake - booksTaken;

            if (booksRemaining <= 0) {
                System.out.println(readerName + " has already taken the maximum allowed number of books.");
                readingRoomSemaphore.release();
                return; // Читатель не может взять больше книг
            }

            List<Book> borrowedBooks = new ArrayList<>();

            for (Book book : books) {
                try {
                    bookSemaphores.get(book).acquire();

                    if (booksRemaining > 0) {
                        if (book.isReadingRoomOnly()) {
                            System.out.println(readerName + " is taking book '" + book.getTitle() + "' to the reading room.");
                        } else {
                            System.out.println(readerName + " is taking book '" + book.getTitle() + "' home.");
                        }

                        borrowedBooks.add(book);
                        booksTakenByReader.put(readerName, booksTaken + 1);
                        booksRemaining--;
                    } else {
                        System.out.println(readerName + " has already taken the maximum allowed number of books.");
                        break; // Читатель не может взять больше книг
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (Book book : borrowedBooks) {
                System.out.println(readerName + " is reading book '" + book.getTitle() + "'.");
                int randomSleepTime = 500 + random.nextInt(1000); // Случайное время сна между 500 мс и 1500 мс
                Thread.sleep(randomSleepTime);
            }

            for (Book book : borrowedBooks) {
                System.out.println(readerName + " is returning book '" + book.getTitle() + "'.");
                bookSemaphores.get(book).release();
            }

            readingRoomSemaphore.release();
            booksTakenByReader.put(readerName, booksTaken - borrowedBooks.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Book {
    private String title;
    private boolean readingRoomOnly;

    public Book(String title, boolean readingRoomOnly) {
        this.title = title;
        this.readingRoomOnly = readingRoomOnly;
    }

    public String getTitle() {
        return title;
    }

    public boolean isReadingRoomOnly() {
        return readingRoomOnly;
    }
}

class LibraryDemo {
    public static void main(String[] args) {
        List<Book> books = new ArrayList<>();
        books.add(new Book("Book 1", false));
        books.add(new Book("Book 2", true));
        books.add(new Book("Book 3", false));

        Library library = new Library(books, 3);

        for (int i = 1; i <= 5; i++) {
            Thread readerThread = new Thread(library::readerThread, "Reader " + i);
            readerThread.start();
        }
    }
}
