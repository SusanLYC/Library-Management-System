package jdbc.LibrarySys;

public class Main {
    public static void main(String[] args) {
        LoggerUtil.getLogger().info("Library Management System Started.");
        System.out.println("Welcome to the Library Management System!");
        
        DatabaseConnection dbConnection = new DatabaseConnection();
        if (dbConnection.loginProxy() && dbConnection.loginDB()) {
            LibraryManager libraryManager = new LibraryManager(dbConnection);
            libraryManager.run();
        }

        dbConnection.closeConnection();
        System.out.println("Goodbye!");
        LoggerUtil.getLogger().info("Library Management System Ended.");
    }
}
