package jdbc.LibrarySys;

import java.util.Scanner;
import java.util.logging.Logger;

public class LibraryManager {

    private static final Logger logger = Logger.getLogger(LibraryManager.class.getName());
    private Scanner in = new Scanner(System.in);
    private DatabaseConnection dbConnection;
    private BookOperations bookOps;

    public LibraryManager(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
        this.bookOps = new BookOperations(dbConnection.getConnection());
    }
    
    /**
     * This is a library manager to support: (1) Book search (2) Book borrow
     * (3) Book return (4) Book renew (5) Book reserve (6) Exit
     */
    public void run() {
        String[] options = { "Book Search", "Book Borrow", "Book Return", "Book Renew", "Book Reserve", "Exit" };
        boolean running = true;

        while (running) {
            showOptions(options);
            String choice = in.nextLine().toLowerCase();

            switch (choice) {
                case "1":
                    System.out.println("Enter ISBN:");
                    String isbn = in.nextLine();
                    bookOps.bookSearch(isbn);
                    break;
                case "2":
                    System.out.println("Enter Student No:");
                    String studentNo = in.nextLine();
                    System.out.println("Enter Book Call No:");
                    String callNo = in.nextLine();
                    bookOps.bookBorrow(studentNo, callNo);
                    break;
                case "3":
                    handleReturn();
                    break;
                case "4":
                    handleRenew();
                    break;
                case "5":
                    handleReserve();
                    break;
                case "exit" :
                case "6":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    /**
     * Show the options. If you want to add one more option, put into the
     * options array above.
     */
    private void showOptions(String[] options) {
        System.out.println("Please choose an option:");
        for (int i = 0; i < options.length; i++) {
            System.out.println((i + 1) + ". " + options[i]);
        }
    }

    // Handle the return operation
    public void handleReturn() {
        System.out.println("Please input Student_no and Call_no of the book to return (format: Student_no, Call_no): ");
        String line = in.nextLine();

        if (!line.equalsIgnoreCase("exit")) {
            String[] values = line.split(",");
            bookOps.bookReturn(values[0].trim(), values[1].trim());
            logger.info("Book returned by Student_no: " + values[0].trim() + ", Call_no: " + values[1].trim());
        }
    }

    // Handle the renew operation
    public void handleRenew() {
        System.out.println("Please input Student_no and Call_no of the book to renew (format: Student_no, Call_no): ");
        String line = in.nextLine();

        if (!line.equalsIgnoreCase("exit")) {
            String[] values = line.split(",");
            bookOps.bookRenew(values[0].trim(), values[1].trim());
            logger.info("Book renewed by Student_no: " + values[0].trim() + ", Call_no: " + values[1].trim());
        }
    }

    // Handle the reserve operation
    public void handleReserve() {
        System.out.println("Please input Student_no and Call_no of the book to reserve (format: Student_no, Call_no): ");
        String line = in.nextLine();

        if (!line.equalsIgnoreCase("exit")) {
            String[] values = line.split(",");
            bookOps.bookReserve(values[0].trim(), values[1].trim());
            logger.info("Book reserved by Student_no: " + values[0].trim() + ", Call_no: " + values[1].trim());
        }
    }
}

