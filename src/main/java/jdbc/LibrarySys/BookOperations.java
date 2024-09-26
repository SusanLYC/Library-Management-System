package jdbc.LibrarySys;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class BookOperations {
    private Connection conn;

    public BookOperations(Connection conn) {
        this.conn = conn;
    }
    
    /**
     * Given a search with ISBN as input,
     * you should tell the availability of the book. If the book is available,
     * display the call_no, ISBN, title, author, amount, and location.
     */
    public void bookSearch(String isbn) {
        LoggerUtil.getLogger().info("Searching for book with ISBN: " + isbn);
 
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT call_no FROM Book WHERE amount > 0 and ISBN ='" + isbn + "'";
            ResultSet rs = stm.executeQuery(sql);
            boolean available = false;

            if (rs.next()) {
                available = true;
                LoggerUtil.getLogger().info("Book with ISBN: " + isbn + "is available.");
                printBookInfo(rs.getString(1));
            }

            if (!available) {
                LoggerUtil.getLogger().warn("Book with ISBN: " + isbn + "is unavailable.");
            }
            rs.close();
            stm.close();
        } catch (SQLException e) {
            LoggerUtil.getLogger().error("Error during book search", e);
        }
    }

    /**
     * Print out the information of a book given a Call_no
     */
    private void printBookInfo(String callNo) {
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT * FROM Book WHERE Call_no = '" + callNo + "'";
            ResultSet rs = stm.executeQuery(sql);
            if (!rs.next()) return;

            String[] heads = { "Call_no", "ISBN", "Title", "Author", "Amount", "Location" };
            for (int i = 0; i < 6; ++i) {
            	System.out.println(heads[i] + " : " + rs.getString(i + 1));
                LoggerUtil.getLogger().info(heads[i] + " : " + rs.getString(i + 1));
            }
        } catch (SQLException e) {
            LoggerUtil.getLogger().error("Error fetching book info", e);
        }
    }
    
    /**
     * Given student A and book B, student A can borrow book B if the following conditions are satisfied:
     * The available amount of B is larger than 0.
     * The amount of books student A held is smaller than 5.
     * None of the books student A borrowed is overdue.
     * Book B is not reserved by any students, or Student A is the first one who
     * reserved B (in this case the corresponding reservation request should be
     * removed after the book is borrowed).
     * The corresponding book and borrow records should be updated upon a successful book borrow/return.
     */
    public void bookBorrow(String studentNo, String callNo) {
        LoggerUtil.getLogger().info("Borrowing book with Call No: " + callNo + " for Student: " + studentNo);
        try {
            Statement stm = conn.createStatement();
            
            // Check if the book is available
            String sql = "SELECT amount FROM Book WHERE call_no ='" + callNo + "'";
            ResultSet rs = stm.executeQuery(sql);

            if (rs.next() && rs.getInt(1) <= 0) {
                LoggerUtil.getLogger().warn("Book is not available for borrowing.");
                return;
            }
            
            // Check if student has already borrowed 5 books
            sql = "SELECT count(*) FROM Borrow_record WHERE student_no ='" + studentNo + "'";
            rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) >= 5) {
                LoggerUtil.getLogger().warn("Student has already borrowed 5 books.");
                return;
            }
            
            // Check if student has any overdue books
            LocalDate date = LocalDate.now();
            sql = "SELECT COUNT(*) FROM Borrow_record WHERE student_no ='" + studentNo + "' AND due_date < TO_DATE('" + date + "','yyyy-mm-dd')";
            rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) > 0) {
            	LoggerUtil.getLogger().warn("Student has overdue books.");
                return;
            }
            
           //check if the student have reserve the book or not
            sql = "select count(*)from Reserve where student_no = '"+studentNo+"' and call_no = '"+callNo+"'";
            rs = stm.executeQuery(sql);
            while(rs.next()) {
                if (rs.getInt(1)==1)
                {   //delete the reserve record since the student have successfully borrowed the book
                    sql = "delete from reserve where student_no = '" + studentNo + "' and call_no = '" + callNo + "'"; 
                    stm.executeUpdate(sql);
                    LoggerUtil.getLogger().debug("Reserve record deleted");
                    sql = "insert into borrow_record values('" + studentNo + "','"+callNo+"',To_DATE('" + date + "','yyyy-mm-dd'), To_DATE('"+date+"','yyyy-mm-dd') +28 ,0)";
                    LoggerUtil.getLogger().debug("Borrow record inserted");
                    LoggerUtil.getLogger().info("Book with Call No: " + callNo + " borrow successful for Student: " + studentNo);
                    System.out.println("The borrow succeeded.");
                    stm.executeUpdate(sql);
                    return;
                }
            }
            
           //check if anyone reserve the book
            sql = "select count(*)from Reserve where call_no = '"+callNo+"'"; 
            rs = stm.executeQuery(sql);
            boolean someoneReserveIt = false;
            while (rs.next())
            {
                if (rs.getInt(1)>0)
                {
                    someoneReserveIt = true;
                }
            }
            //search for the available copies of the book
            sql = "select amount from book where call_no = '"+callNo+"'"; 
            rs = stm.executeQuery(sql);
            boolean amountOnlyOne = false;
            while(rs.next())
            {
                if(rs.getInt(1) == 1)
                {
                    amountOnlyOne = true;
                }
            }
            //when there are only 1 copy left and somebody else have reserve it already
            if(someoneReserveIt && amountOnlyOne) 
            {
            	System.out.println("The book is reserved by someone else. You cannot borrow it.");
                LoggerUtil.getLogger().info("Book with Call No: " + callNo + " unavaliable: reserved by someone else.");
                return;
            }
            
            // Record the borrow
            sql = "INSERT INTO Borrow_record VALUES('" + studentNo + "', '" + callNo + "', TO_DATE('" + date + "','yyyy-mm-dd'), TO_DATE('" + date + "','yyyy-mm-dd') + 28 ,0)";
            stm.executeUpdate(sql);
            System.out.println("The borrow succeeded.");
            LoggerUtil.getLogger().info("Book with Call No: " + callNo + " borrow successful for Student: " + studentNo);
        } catch (SQLException e) {
            LoggerUtil.getLogger().error("Error during book borrow", e);
        }
    }
    
	/**
	 * Given student A and book B.
	 * Student A can return book B if Book B is borrowed by student A. 
	 * The corresponding borrow record should be updated upon success.
	 */
    public void bookReturn(String studentNo, String callNo) {
        try {
            Statement stm = conn.createStatement();
            String sql = "DELETE FROM Borrow_record WHERE Student_no = '" + studentNo + "' AND CALL_NO = '" + callNo + "'";
            ResultSet rs = stm.executeQuery(sql);
            LoggerUtil.getLogger().info("Book with Call No: " + callNo + " returned by Student: " + studentNo);
            System.out.println("The return succeeded.");
            rs.close();
            stm.close();
        } catch (SQLException e) {
        	LoggerUtil.getLogger().error("Error during book return", e);
        }
    }
    
    /**
     * Given student A and book B, student A can renew book B if the following conditions are satisfied:
     * None of the books student A borrowed is overdue.
     * Student A has not renewed book B after he borrowed it.
     * This renewal is allowed only during the 2nd half of B’s borrow period.
     * Book B is not reserved by any students.
     * The corresponding borrow record should be updated upon success.
     */
    public void bookRenew(String studentNo, String callNo) {
        try {
            LocalDate date = LocalDate.now();
            Statement stm = conn.createStatement();

            // Check if student has any overdue books
            String sql = "SELECT COUNT(*) FROM Borrow_record WHERE student_no ='" + studentNo + "' AND due_date < TO_DATE('" + date + "','yyyy-mm-dd')";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("You have overdue books!");
                LoggerUtil.getLogger().warn("Student: " + studentNo + " has " + rs.getInt(1) + " overdue books.");
                return;
            }

            // Check if the book has already been renewed
            sql = "SELECT Renewed_bit FROM Borrow_record WHERE Call_no='" + callNo + "' AND Student_no='" + studentNo + "'";
            rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) == 1) {
            	LoggerUtil.getLogger().warn("Book with Call No: " + callNo + " has already been renewed by Student: " + studentNo);
                System.out.println("This book has already been renewed!");
                return;
            }

            // Check if the book is available for renewal
            sql = "SELECT * FROM Borrow_record WHERE Call_no='" + callNo + "' AND Student_no='" + studentNo + "'";
            rs = stm.executeQuery(sql);
            if (rs.next()) {
                Date borrowDate = rs.getDate(3);
                Date secondHalfDate = new Date(borrowDate.getTime() + TimeUnit.DAYS.toMillis(14));
                Date localDate = new SimpleDateFormat("yyyy-MM-dd").parse(date.toString());

                if (secondHalfDate.after(localDate)) {
                	// Log the book, student, and date
                	LoggerUtil.getLogger().warn("Book with Call No: " + callNo + " can only be renewed after " + date);
                    System.out.println("Renewing the book is only available in the second half of the borrow period, after: " + secondHalfDate);
                    return;
                }
            }

            // Check if the book is reserved by another student
            sql = "SELECT COUNT(*) FROM Reserve WHERE Call_no='" + callNo + "'";
            rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) > 0) {
            	LoggerUtil.getLogger().warn("Book with Call No: " + callNo + " is reserved by another student.");
                System.out.println("This book has already been reserved by another student.");
                return;
            }
            
            // Renew the book
            sql = "UPDATE Borrow_record SET due_date = due_date + 14, renewed_bit = 1 WHERE Call_no = '" + callNo + "' AND Student_no = '" + studentNo + "'";
            stm.executeQuery(sql);
            LoggerUtil.getLogger().info("Book with Call No: " + callNo + " renewed by Student: " + studentNo);
            System.out.println("Book renewal is successful.");
            rs.close();
            stm.close();
        } catch (SQLException | ParseException e) {
            LoggerUtil.getLogger().error("Error during book renew", e);
        }
    }

    /** Given student A and book B, student A can reserve book B if the
     * following conditions are satisfied:
     * The available amount of B is 0.
     * Book B is not borrowed by Student A.
     * Student A does not hold any other reservation request.
     * The corresponding reserve record should be updated upon success. */
    public void bookReserve(String studentNo, String callNo) {
        try {
            Statement stm = conn.createStatement();

            String sql = "SELECT amount FROM Book WHERE Call_no='" + callNo + "'";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) > 0) {
            	LoggerUtil.getLogger().warn("Book with Call No: " + callNo + " is available.");
                System.out.println("The book is available. No reservation is required.");
                return;
            }

            sql = "SELECT Student_no FROM Borrow_Record WHERE Call_no ='" + callNo + "'";
            rs = stm.executeQuery(sql);
            if (rs.next() && rs.getString(1).equalsIgnoreCase(studentNo)) {
            	LoggerUtil.getLogger().warn("Reserve unavaliable. Student: " + studentNo + " has already borrowed the book.");
                System.out.println("You cannot reserve a borrowed book.");
                return;
            }

            sql = "SELECT COUNT(*) FROM Reserve WHERE Student_no ='" + studentNo + "'";
            rs = stm.executeQuery(sql);
            if (rs.next() && rs.getInt(1) >= 1) {
            	LoggerUtil.getLogger().warn("Reserve unavaliable. Student: " + studentNo + " has already reserved a book.");
                System.out.println("Multiple reservations are not allowed.");
                return;
            }

            LocalDate date = LocalDate.now();
            sql = "INSERT INTO Reserve VALUES('" + studentNo + "','" + callNo + "',TO_DATE('" + date + "','yyyy-mm-dd'))";
            stm.executeQuery(sql);
            LoggerUtil.getLogger().info("Book with Call No: " + callNo + " reserved by Student: " + studentNo);
            System.out.println("The reservation succeeded.");
            rs.close();
            stm.close();
        } catch (SQLException e) {
            LoggerUtil.getLogger().error("Error during book reserve", e);
        }
    }

}
