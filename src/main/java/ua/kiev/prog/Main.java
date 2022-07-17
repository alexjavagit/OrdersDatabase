package ua.kiev.prog;

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/my_db?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    static final String DB_USER = "bestuser";
    static final String DB_PASSWORD = "Delusion77";

    static Connection conn;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            try {
                // create connection
                conn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
                initDB();

                while (true) {
                    System.out.println("1: add new client");
                    System.out.println("2: add new product");
                    System.out.println("3: create order");
                    System.out.println("4: view orders");
                    System.out.print("-> ");

                    String s = sc.nextLine();
                    switch (s) {
                        case "1":
                            addClient(sc);
                            break;
                        case "2":
                            addProduct(sc);
                            break;
                        case "3":
                            createOrder(sc);
                            break;
                        case "4":
                            viewOrders();
                            break;
                        default:
                            return;
                    }
                }
            } finally {
                sc.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }
    }

    private static void initDB() throws SQLException {
        Statement st = conn.createStatement();
        try {
            st.execute("DROP TABLE IF EXISTS Products");
            st.execute("CREATE TABLE Products (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL, price INT(6))");

            st.execute("DROP TABLE IF EXISTS Clients");
            st.execute("CREATE TABLE Clients (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(200) NOT NULL)");

            st.execute("DROP TABLE IF EXISTS Orders");
            st.execute("CREATE TABLE Orders (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, client_id INT(12) NOT NULL DEFAULT 0, date_added DATE NOT NULL DEFAULT (CURRENT_DATE))");

            st.execute("DROP TABLE IF EXISTS Orders_products");
            st.execute("CREATE TABLE Orders_products (id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, order_id INT(12) NOT NULL DEFAULT 0, product_id INT(12) NOT NULL DEFAULT 0, pcount INT(3) NOT NULL DEFAULT 0)");

        } finally {
            st.close();
        }
    }

    private static void addClient(Scanner sc) throws SQLException {
        System.out.print("Enter client name: ");
        String name = sc.nextLine();

        PreparedStatement ps = conn.prepareStatement("INSERT INTO Clients (name) VALUES(?)");
        try {
            ps.setString(1, name);
            ps.executeUpdate();
            System.out.println("Client added!");
        } finally {
            ps.close();
        }
    }

    private static void addProduct(Scanner sc) throws SQLException {
        System.out.print("Enter product name: ");
        String name = sc.nextLine();
        System.out.print("Enter product price: ");
        String sPrice = sc.nextLine();
        int price = Integer.parseInt(sPrice);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO Products (name, price) VALUES(?,?)");
        try {
            ps.setString(1, name);
            ps.setInt(2, price);
            ps.executeUpdate();
            System.out.println("Product added!");
        } finally {
            ps.close();
        }
    }

    private static void createOrder(Scanner sc) throws SQLException {
        int clientId;
        int productId;
        int primkey = 0;

        System.out.print("Enter client name: ");
        String name = sc.nextLine();
        System.out.print("Enter product name: ");
        String pname = sc.nextLine();
        System.out.print("Enter product quantity: ");
        String sQty = sc.nextLine();
        int qty = Integer.parseInt(sQty);
        if (qty==0) qty = 1;

        PreparedStatement cl = conn.prepareStatement("SELECT id FROM Clients WHERE name = ?");
        cl.setString(1, name);
        ResultSet rs = cl.executeQuery();
        if (rs.next()) {
            clientId = rs.getInt(1);
            cl.close();
        } else {
            System.out.println("Can't create new order! Wrong client name!");
            cl.close();
           return;
        }

        PreparedStatement pr = conn.prepareStatement("SELECT id FROM Products WHERE name = ?");
        pr.setString(1, pname);
        rs = pr.executeQuery();
        if (rs.next()) {
            productId = rs.getInt(1);
            pr.close();
        } else {
            System.out.println("Can't create new order! Wrong product name!");
            pr.close();
            return;
        }

        PreparedStatement ps = conn.prepareStatement("INSERT INTO Orders (client_id, date_added) VALUES(?, NOW())",  Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, clientId);
        ps.executeUpdate();
        java.sql.ResultSet generatedKeys = ps.getGeneratedKeys();
        if ( generatedKeys.next() ) {
            primkey = generatedKeys.getInt(1);
        }

        ps = conn.prepareStatement("INSERT INTO Orders_products (order_id, product_id, pcount) VALUES(?, ?, ?)");
        ps.setInt(1, primkey);
        ps.setInt(2, productId);
        ps.setInt(3, qty);

        ps.executeUpdate();
    }


    private static void viewOrders() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT Orders.id, Clients.name AS client_name, Products.name AS product_name, Orders_products.pcount, DATE_FORMAT(Orders.date_added, '%d/%m/%Y') AS odate FROM Orders, Orders_products, Clients, Products WHERE Orders.id=Orders_products.order_id AND Orders_products.product_id=Products.id AND Orders.client_id=Clients.id ORDER BY Orders.date_added DESC");
        try {
            // table of data representing a database result set,
            ResultSet rs = ps.executeQuery();
            try {
                // can be used to get information about the types and properties of the columns in a ResultSet object
                ResultSetMetaData md = rs.getMetaData();

                for (int i = 1; i <= md.getColumnCount(); i++)
                    System.out.print(md.getColumnName(i) + "\t\t");
                System.out.println();

                while (rs.next()) {
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        System.out.print(rs.getString(i) + "\t\t");
                    }
                    System.out.println();
                }
            } finally {
                rs.close(); // rs can't be null according to the docs
            }
        } finally {
            ps.close();
        }
    }
}

