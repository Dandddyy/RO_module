package main

import (
	"fmt"
	"math/rand"
	"sync"
	"time"
)

type Book struct {
	title           string
	readingRoomOnly bool
}

type Library struct {
	books                []*Book
	readingRoomSemaphore chan struct{}
	bookSemaphores       map[*Book]chan struct{}
	random               *rand.Rand
	booksTakenByReader   map[string]int
	mu                   sync.Mutex
}

func NewLibrary(books []*Book, maxReadersInRoom int) *Library {
	library := &Library{
		books:                books,
		readingRoomSemaphore: make(chan struct{}, maxReadersInRoom),
		bookSemaphores:       make(map[*Book]chan struct{}),
		random:               rand.New(rand.NewSource(time.Now().UnixNano())),
		booksTakenByReader:   make(map[string]int),
	}

	for _, book := range books {
		library.bookSemaphores[book] = make(chan struct{}, 1)
	}

	return library
}

func (l *Library) readerThread(wg *sync.WaitGroup) {
	defer wg.Done()
	l.readingRoomSemaphore <- struct{}{}
	readerName := "Reader " + fmt.Sprintf("%d", time.Now().UnixNano())

	maxBooksToTake := 2
	booksTaken, exists := l.booksTakenByReader[readerName]
	if !exists {
		l.booksTakenByReader[readerName] = 0
	}
	booksRemaining := maxBooksToTake - booksTaken

	if booksRemaining <= 0 {
		fmt.Printf("%s has already taken the maximum allowed number of books.\n", readerName)
		<-l.readingRoomSemaphore
		return
	}

	borrowedBooks := make([]*Book, 0)

	for _, book := range l.books {
		bookSemaphore := l.bookSemaphores[book]
		bookSemaphore <- struct{}{}

		if booksRemaining > 0 {
			if book.readingRoomOnly {
				fmt.Printf("%s is taking book '%s' to the reading room.\n", readerName, book.title)
			} else {
				fmt.Printf("%s is taking book '%s' home.\n", readerName, book.title)
			}

			borrowedBooks = append(borrowedBooks, book)
			l.booksTakenByReader[readerName] = booksTaken + 1
			booksRemaining--
		} else {
			fmt.Printf("%s has already taken the maximum allowed number of books.\n", readerName)
			<-bookSemaphore
			break
		}
		<-bookSemaphore
	}

	for _, book := range borrowedBooks {
		fmt.Printf("%s is reading book '%s'.\n", readerName, book.title)
		randomSleepTime := 500 + l.random.Intn(1000)
		time.Sleep(time.Duration(randomSleepTime) * time.Millisecond)
	}

	for _, book := range borrowedBooks {
		fmt.Printf("%s is returning book '%s'.\n", readerName, book.title)
	}

	<-l.readingRoomSemaphore

	l.mu.Lock()
	l.booksTakenByReader[readerName] = booksTaken - len(borrowedBooks)
	l.mu.Unlock()
}

func main() {
	books := []*Book{
		{"Book 1", false},
		{"Book 2", true},
		{"Book 3", false},
	}

	library := NewLibrary(books, 3)
	var wg sync.WaitGroup

	for i := 1; i <= 5; i++ {
		wg.Add(1)
		go library.readerThread(&wg)
	}

	wg.Wait()
}
