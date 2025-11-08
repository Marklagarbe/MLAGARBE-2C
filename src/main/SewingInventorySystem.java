package main;

import config.config;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class SewingInventorySystem {
    
    private static Scanner scanner = new Scanner(System.in);
    private static config conf = new config();
    private static User currentUser = null;
    
    // ==================== USER CLASS ====================
    static class User {
        int userId;
        String username;
        String fullName;
        String email;
        String phone;
        String address;
        String role;
        String status;
        
        boolean isAdmin() { return "admin".equals(role); }
        boolean isManager() { return "manager".equals(role); }
        boolean isStaff() { return "staff".equals(role); }
        boolean isUser() { return "user".equals(role); }
        boolean isApproved() { return "approved".equals(status); }
    }
    
    // ==================== MAIN METHOD ====================
    public static void main(String[] args) {
        // Initialize database
        createTables();
        
        // Main menu loop
        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                if (currentUser.isAdmin()) {
                    showAdminMenu();
                } else if (currentUser.isManager()) {
                    showManagerMenu();
                } else if (currentUser.isStaff()) {
                    showStaffMenu();
                } else {
                    showUserMenu();
                }
            }
        }
    }
    
    // ==================== DATABASE SETUP ====================
    private static void createTables() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = conf.connectDB();
            stmt = conn.createStatement();
            
            // Create USERS table
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "full_name TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "phone TEXT, " +
                "address TEXT, " +
                "role TEXT DEFAULT 'user', " +
                "status TEXT DEFAULT 'pending', " +
                "created_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")";
            stmt.execute(createUsersTable);
            
            // Create SUPPLIERS table
            String createSuppliersTable = "CREATE TABLE IF NOT EXISTS suppliers (" +
                "supplier_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "supplier_name TEXT NOT NULL, " +
                "contact_person TEXT, " +
                "email TEXT, " +
                "phone TEXT, " +
                "address TEXT, " +
                "website TEXT" +
            ")";
            stmt.execute(createSuppliersTable);
            
            // Create ITEMS table
            String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                "item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_name TEXT NOT NULL, " +
                "item_type TEXT NOT NULL, " +
                "category TEXT, " +
                "quantity REAL DEFAULT 0, " +
                "unit TEXT, " +
                "price REAL, " +
                "reorder_level REAL, " +
                "supplier_id INTEGER, " +
                "storage_location TEXT, " +
                "last_ordered_date DATETIME, " +
                "FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)" +
            ")";
            stmt.execute(createItemsTable);
            
            // Create PURCHASE_ORDERS table
            String createPurchaseOrdersTable = "CREATE TABLE IF NOT EXISTS purchase_orders (" +
                "purchase_order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "supplier_id INTEGER NOT NULL, " +
                "order_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "expected_delivery_date DATETIME, " +
                "total_amount REAL, " +
                "status TEXT DEFAULT 'pending', " +
                "notes TEXT, " +
                "created_by INTEGER, " +
                "FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id), " +
                "FOREIGN KEY (created_by) REFERENCES users(user_id)" +
            ")";
            stmt.execute(createPurchaseOrdersTable);
            
            // Create PURCHASE_ORDER_ITEMS table
            String createPurchaseOrderItemsTable = "CREATE TABLE IF NOT EXISTS purchase_order_items (" +
                "po_item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "purchase_order_id INTEGER NOT NULL, " +
                "item_id INTEGER NOT NULL, " +
                "quantity REAL NOT NULL, " +
                "unit_price REAL NOT NULL, " +
                "subtotal REAL NOT NULL, " +
                "FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(purchase_order_id), " +
                "FOREIGN KEY (item_id) REFERENCES items(item_id)" +
            ")";
            stmt.execute(createPurchaseOrderItemsTable);
            
            // Create ORDERS table
            String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "order_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "total_amount REAL, " +
                "status TEXT DEFAULT 'pending', " +
                "payment_method TEXT, " +
                "notes TEXT, " +
                "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
            ")";
            stmt.execute(createOrdersTable);
            
            // Create ORDER_ITEMS table
            String createOrderItemsTable = "CREATE TABLE IF NOT EXISTS order_items (" +
                "order_item_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "order_id INTEGER NOT NULL, " +
                "item_id INTEGER NOT NULL, " +
                "quantity REAL NOT NULL, " +
                "unit_price REAL NOT NULL, " +
                "subtotal REAL NOT NULL, " +
                "FOREIGN KEY (order_id) REFERENCES orders(order_id), " +
                "FOREIGN KEY (item_id) REFERENCES items(item_id)" +
            ")";
            stmt.execute(createOrderItemsTable);
            
            System.out.println("Database initialized successfully!");
            
        } catch (Exception e) {
            System.out.println("Error creating tables: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
        
        insertDefaultAdmin();
    }
    
    private static void insertDefaultAdmin() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            Thread.sleep(100);
            
            conn = conf.connectDB();
            stmt = conn.createStatement();
            
            String checkAdmin = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
            rs = stmt.executeQuery(checkAdmin);
            
            if (rs.next() && rs.getInt(1) == 0) {
                rs.close();
                stmt.close();
                conn.close();
                
                String sql = "INSERT INTO users (username, password, full_name, email, role, status) " +
                           "VALUES (?, ?, ?, ?, ?, ?)";
                String hashedAdminPassword = hashPassword("admin123");
                conf.addRecord(sql, "admin", hashedAdminPassword, "System Administrator", 
                             "admin@sewing.com", "admin", "approved");
                System.out.println("Default admin created: username=admin, password=admin123");
            }
        } catch (Exception e) {
            System.out.println("Error checking/creating admin: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    // ==================== LOGIN/REGISTER MENU ====================
    private static void showLoginMenu() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║  SEWING INVENTORY MANAGEMENT SYSTEM  ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                handleLogin();
                break;
            case 2:
                handleRegister();
                break;
            case 3:
                System.out.println("Thank you for using the system!");
                System.exit(0);
            default:
                System.out.println("Invalid option!");
        }
    }
    
    // ==================== PASSWORD HASHING ====================
    private static String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.out.println("Error hashing password: " + e.getMessage());
            return password;
        }
    }
    
    // ==================== LOGIN ====================
    private static void handleLogin() {
        System.out.println("\n=== LOGIN ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        String hashedPassword = hashPassword(password);
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                
                if ("pending".equals(status)) {
                    System.out.println("⏳ Your account is pending admin approval.");
                    return;
                } else if ("rejected".equals(status)) {
                    System.out.println("❌ Your account has been rejected by admin.");
                    return;
                }
                
                currentUser = new User();
                currentUser.userId = rs.getInt("user_id");
                currentUser.username = rs.getString("username");
                currentUser.fullName = rs.getString("full_name");
                currentUser.email = rs.getString("email");
                currentUser.phone = rs.getString("phone");
                currentUser.address = rs.getString("address");
                currentUser.role = rs.getString("role");
                currentUser.status = status;
                
                System.out.println("✓ Login successful! Welcome " + currentUser.fullName);
            } else {
                System.out.println("❌ Invalid username or password!");
            }
            
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }
    
    // ==================== REGISTER ====================
    private static void handleRegister() {
        System.out.println("\n=== REGISTRATION ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        
        if (usernameExists(username)) {
            System.out.println("❌ Username already exists!");
            return;
        }
        
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        
        if (emailExists(email)) {
            System.out.println("❌ Email already exists!");
            return;
        }
        
        System.out.print("Phone: ");
        String phone = scanner.nextLine();
        System.out.print("Address: ");
        String address = scanner.nextLine();
        
        System.out.println("\nSelect Role:");
        System.out.println("1. User (Customer)");
        System.out.println("2. Staff (Employee)");
        System.out.println("3. Manager");
        System.out.print("Choose role: ");
        int roleChoice = scanner.nextInt();
        scanner.nextLine();
        
        String role = "user";
        switch (roleChoice) {
            case 1:
                role = "user";
                break;
            case 2:
                role = "staff";
                break;
            case 3:
                role = "manager";
                break;
            default:
                System.out.println("Invalid choice! Defaulting to User role.");
                role = "user";
        }
        
        try {
            String hashedPassword = hashPassword(password);
            String sql = "INSERT INTO users (username, password, full_name, email, phone, address, role, status) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')";
            conf.addRecord(sql, username, hashedPassword, fullName, email, phone, address, role);
            
            System.out.println("✓ Registration successful! Waiting for admin approval.");
            System.out.println("Role: " + role.toUpperCase());
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }
    
    private static boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.out.println("Error checking username: " + e.getMessage());
        }
        return false;
    }
    
    private static boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            System.out.println("Error checking email: " + e.getMessage());
        }
        return false;
    }
    
    // ==================== ADMIN MENU ====================
    private static void showAdminMenu() {
        int pendingCount = getPendingUserCount();
        
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║         ADMIN DASHBOARD              ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("Welcome, " + currentUser.fullName);
        if (pendingCount > 0) {
            System.out.println("⚠️  You have " + pendingCount + " pending user(s) for approval!");
        }
        System.out.println("\n1. View Pending Users");
        System.out.println("2. View All Users");
        System.out.println("3. Approve User");
        System.out.println("4. Reject User");
        System.out.println("5. Delete User");
        System.out.println("6. Manage Suppliers");
        System.out.println("7. Manage Inventory");
        System.out.println("8. Manage Orders");
        System.out.println("9. View Reports");
        System.out.println("10. Logout");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                viewPendingUsers();
                break;
            case 2:
                viewAllUsers();
                break;
            case 3:
                System.out.print("Enter User ID to approve: ");
                int approveId = scanner.nextInt();
                scanner.nextLine();
                approveUser(approveId);
                break;
            case 4:
                System.out.print("Enter User ID to reject: ");
                int rejectId = scanner.nextInt();
                scanner.nextLine();
                rejectUser(rejectId);
                break;
            case 5:
                System.out.print("Enter User ID to delete: ");
                int deleteId = scanner.nextInt();
                scanner.nextLine();
                deleteUser(deleteId);
                break;
            case 6:
                showSupplierMenu();
                break;
            case 7:
                showInventoryMenu();
                break;
            case 8:
                showOrderMenu();
                break;
            case 9:
                showReportsMenu();
                break;
            case 10:
                currentUser = null;
                System.out.println("✓ Logged out successfully!");
                break;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    private static void viewPendingUsers() {
        String sql = "SELECT user_id, username, full_name, email, phone, created_date " +
                   "FROM users WHERE status = 'pending' ORDER BY created_date DESC";
        String[] headers = {"User ID", "Username", "Full Name", "Email", "Phone", "Created Date"};
        String[] columns = {"user_id", "username", "full_name", "email", "phone", "created_date"};
        
        System.out.println("\n=== PENDING USER REGISTRATIONS ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void viewAllUsers() {
        String sql = "SELECT user_id, username, full_name, email, role, status, created_date " +
                   "FROM users ORDER BY created_date DESC";
        String[] headers = {"User ID", "Username", "Full Name", "Email", "Role", "Status", "Created"};
        String[] columns = {"user_id", "username", "full_name", "email", "role", "status", "created_date"};
        
        System.out.println("\n=== ALL USERS ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void approveUser(int userId) {
        String sql = "UPDATE users SET status = 'approved' WHERE user_id = ?";
        conf.updateRecord(sql, userId);
        System.out.println("✓ User approved successfully!");
    }
    
    private static void rejectUser(int userId) {
        String sql = "UPDATE users SET status = 'rejected' WHERE user_id = ?";
        conf.updateRecord(sql, userId);
        System.out.println("✓ User rejected!");
    }
    
    private static void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        conf.deleteRecord(sql, userId);
        System.out.println("✓ User deleted successfully!");
    }
    
    private static int getPendingUserCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE status = 'pending'";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Error getting pending count: " + e.getMessage());
        }
        return 0;
    }
    
    // ==================== MANAGER MENU ====================
    private static void showManagerMenu() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║        MANAGER DASHBOARD             ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("Welcome, " + currentUser.fullName + " (Manager)");
        System.out.println("\n1. Manage Inventory");
        System.out.println("2. Manage Suppliers");
        System.out.println("3. Manage Orders (Customer)");
        System.out.println("4. Create Purchase Order (from Suppliers)");
        System.out.println("5. View Purchase Orders");
        System.out.println("6. Update Order Status");
        System.out.println("7. View Low Stock Items");
        System.out.println("8. Generate Reports");
        System.out.println("9. View Staff Performance");
        System.out.println("10. Logout");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                showInventoryMenu();
                break;
            case 2:
                showSupplierMenu();
                break;
            case 3:
                showOrderMenu();
                break;
            case 4:
                createPurchaseOrder();
                break;
            case 5:
                viewAllPurchaseOrders();
                break;
            case 6:
                updateOrderStatus();
                break;
            case 7:
                viewLowStockItems();
                break;
            case 8:
                showReportsMenu();
                break;
            case 9:
                viewStaffPerformance();
                break;
            case 10:
                currentUser = null;
                System.out.println("✓ Logged out successfully!");
                break;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    // ==================== PURCHASE ORDER METHODS ====================
    private static void createPurchaseOrder() {
        System.out.println("\n=== CREATE PURCHASE ORDER (Order from Supplier) ===");
        
        viewAllSuppliers();
        System.out.print("Enter Supplier ID: ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("Expected Delivery Date (YYYY-MM-DD): ");
        String deliveryDate = scanner.nextLine();
        System.out.print("Notes: ");
        String notes = scanner.nextLine();
        
        // Create purchase order
        String poSql = "INSERT INTO purchase_orders (supplier_id, expected_delivery_date, notes, total_amount, status, created_by) " +
                      "VALUES (?, ?, ?, 0, 'pending', ?)";
        conf.addRecord(poSql, supplierId, deliveryDate, notes, currentUser.userId);
        
        int purchaseOrderId = getLastInsertedPurchaseOrderId();
        
        if (purchaseOrderId > 0) {
            System.out.println("✓ Purchase Order created with ID: " + purchaseOrderId);
            
            boolean addingItems = true;
            double totalAmount = 0;
            
            while (addingItems) {
                viewAllItems();
                System.out.print("\nEnter Item ID to order (0 to finish): ");
                int itemId = scanner.nextInt();
                
                if (itemId == 0) {
                    addingItems = false;
                    continue;
                }
                
                System.out.print("Enter Quantity to order: ");
                double quantity = scanner.nextDouble();
                System.out.print("Enter Unit Price: ");
                double unitPrice = scanner.nextDouble();
                scanner.nextLine();
                
                double subtotal = quantity * unitPrice;
                totalAmount += subtotal;
                
                String itemSql = "INSERT INTO purchase_order_items (purchase_order_id, item_id, quantity, unit_price, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?)";
                conf.addRecord(itemSql, purchaseOrderId, itemId, quantity, unitPrice, subtotal);
                
                System.out.println("✓ Item added to purchase order! Subtotal: ₱" + subtotal);
            }
            
            if (totalAmount > 0) {
                String updateTotalSql = "UPDATE purchase_orders SET total_amount = ? WHERE purchase_order_id = ?";
                conf.updateRecord(updateTotalSql, totalAmount, purchaseOrderId);
                
                System.out.println("\n✓ Purchase Order created successfully!");
                System.out.println("PO ID: " + purchaseOrderId);
                System.out.println("Total Amount: ₱" + totalAmount);
            } else {
                String deleteSql = "DELETE FROM purchase_orders WHERE purchase_order_id = ?";
                conf.deleteRecord(deleteSql, purchaseOrderId);
                System.out.println("Purchase order cancelled - no items added.");
            }
        }
    }
    
    private static void viewAllPurchaseOrders() {
        String sql = "SELECT po.purchase_order_id, s.supplier_name, po.order_date, " +
                   "po.expected_delivery_date, po.total_amount, po.status " +
                   "FROM purchase_orders po " +
                   "JOIN suppliers s ON po.supplier_id = s.supplier_id " +
                   "ORDER BY po.order_date DESC";
        String[] headers = {"PO ID", "Supplier", "Order Date", "Expected Delivery", "Total Amount", "Status"};
        String[] columns = {"purchase_order_id", "supplier_name", "order_date", "expected_delivery_date", "total_amount", "status"};
        
        System.out.println("\n=== ALL PURCHASE ORDERS ===");
        conf.viewRecords(sql, headers, columns);
        
        System.out.println("\nOptions:");
        System.out.println("1. View PO Details");
        System.out.println("2. Update PO Status");
        System.out.println("3. Receive Items (Update Inventory)");
        System.out.println("4. Back");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                System.out.print("Enter PO ID: ");
                int poId = scanner.nextInt();
                scanner.nextLine();
                viewPurchaseOrderDetails(poId);
                break;
            case 2:
                updatePurchaseOrderStatus();
                break;
            case 3:
                receiveItemsFromPO();
                break;
            case 4:
                return;
        }
    }
    
    private static void viewPurchaseOrderDetails(int poId) {
        String sql = "SELECT poi.po_item_id, i.item_name, poi.quantity, poi.unit_price, poi.subtotal " +
                   "FROM purchase_order_items poi " +
                   "JOIN items i ON poi.item_id = i.item_id " +
                   "WHERE poi.purchase_order_id = ?";
        String[] headers = {"Item ID", "Item Name", "Quantity", "Unit Price", "Subtotal"};
        String[] columns = {"po_item_id", "item_name", "quantity", "unit_price", "subtotal"};
        
        System.out.println("\n=== PURCHASE ORDER DETAILS (PO #" + poId + ") ===");
        conf.viewRecords(sql, headers, columns, poId);
    }
    
    private static void updatePurchaseOrderStatus() {
        System.out.print("Enter Purchase Order ID: ");
        int poId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.println("1. Pending");
        System.out.println("2. Ordered");
        System.out.println("3. Delivered");
        System.out.println("4. Cancelled");
        System.out.print("Choose status: ");
        int statusChoice = scanner.nextInt();
        scanner.nextLine();
        
        String status = "";
        switch (statusChoice) {
            case 1: status = "pending"; break;
            case 2: status = "ordered"; break;
            case 3: status = "delivered"; break;
            case 4: status = "cancelled"; break;
            default:
                System.out.println("Invalid status!");
                return;
        }
        
        String sql = "UPDATE purchase_orders SET status = ? WHERE purchase_order_id = ?";
        conf.updateRecord(sql, status, poId);
        System.out.println("✓ Purchase Order status updated to: " + status);
    }
    
    private static void receiveItemsFromPO() {
        System.out.print("Enter Purchase Order ID to receive: ");
        int poId = scanner.nextInt();
        scanner.nextLine();
        
        // Get all items from PO
        String sql = "SELECT poi.item_id, i.item_name, poi.quantity " +
                   "FROM purchase_order_items poi " +
                   "JOIN items i ON poi.item_id = i.item_id " +
                   "WHERE poi.purchase_order_id = ?";
        
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, poId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n=== Items to Receive ===");
            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String itemName = rs.getString("item_name");
                double quantity = rs.getDouble("quantity");
                
                System.out.println("Item: " + itemName + " | Quantity: " + quantity);
                
                // Update inventory
                updateItemQuantity(itemId, quantity, true); // true = add to inventory
            }
            
            // Update PO status to delivered
            String updateSql = "UPDATE purchase_orders SET status = 'delivered' WHERE purchase_order_id = ?";
            conf.updateRecord(updateSql, poId);
            
            System.out.println("\n✓ All items received and inventory updated!");
            
        } catch (Exception e) {
            System.out.println("Error receiving items: " + e.getMessage());
        }
    }
    
    private static int getLastInsertedPurchaseOrderId() {
        String sql = "SELECT MAX(purchase_order_id) as last_id FROM purchase_orders";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("last_id");
            }
        } catch (Exception e) {
            System.out.println("Error getting last PO ID: " + e.getMessage());
        }
        return 0;
    }
    
    // ==================== STAFF MENU ====================
    private static void showStaffMenu() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║         STAFF DASHBOARD              ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("Welcome, " + currentUser.fullName + " (Staff)");
        System.out.println("\n1. View Available Items");
        System.out.println("2. Process Orders");
        System.out.println("3. View All Orders");
        System.out.println("4. Update Order Status");
        System.out.println("5. View Low Stock Items");
        System.out.println("6. My Profile");
        System.out.println("7. Logout");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                viewAllItems();
                break;
            case 2:
                createOrder();
                break;
            case 3:
                viewAllOrders();
                break;
            case 4:
                updateOrderStatus();
                break;
            case 5:
                viewLowStockItems();
                break;
            case 6:
                showUserProfile();
                break;
            case 7:
                currentUser = null;
                System.out.println("✓ Logged out successfully!");
                break;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    // ==================== REPORTS MENU ====================
    private static void showReportsMenu() {
        System.out.println("\n=== REPORTS & ANALYTICS ===");
        System.out.println("1. Sales Report");
        System.out.println("2. Inventory Report");
        System.out.println("3. Order Status Summary");
        System.out.println("4. Top Selling Items");
        System.out.println("5. Revenue Report");
        System.out.println("6. Back");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                generateSalesReport();
                break;
            case 2:
                generateInventoryReport();
                break;
            case 3:
                generateOrderStatusSummary();
                break;
            case 4:
                generateTopSellingItems();
                break;
            case 5:
                generateRevenueReport();
                break;
            case 6:
                return;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    private static void generateSalesReport() {
        String sql = "SELECT DATE(order_date) as sale_date, COUNT(*) as total_orders, " +
                   "SUM(total_amount) as total_sales FROM orders " +
                   "WHERE status = 'completed' GROUP BY DATE(order_date) ORDER BY sale_date DESC LIMIT 10";
        String[] headers = {"Date", "Total Orders", "Total Sales"};
        String[] columns = {"sale_date", "total_orders", "total_sales"};
        
        System.out.println("\n=== SALES REPORT (Last 10 Days) ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void generateInventoryReport() {
        String sql = "SELECT item_type, COUNT(*) as total_items, SUM(quantity) as total_quantity, " +
                   "SUM(quantity * price) as inventory_value FROM items GROUP BY item_type";
        String[] headers = {"Item Type", "Total Items", "Total Quantity", "Inventory Value"};
        String[] columns = {"item_type", "total_items", "total_quantity", "inventory_value"};
        
        System.out.println("\n=== INVENTORY REPORT BY TYPE ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void generateOrderStatusSummary() {
        String sql = "SELECT status, COUNT(*) as order_count, SUM(total_amount) as total_amount " +
                   "FROM orders GROUP BY status";
        String[] headers = {"Status", "Order Count", "Total Amount"};
        String[] columns = {"status", "order_count", "total_amount"};
        
        System.out.println("\n=== ORDER STATUS SUMMARY ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void generateTopSellingItems() {
        String sql = "SELECT i.item_name, SUM(oi.quantity) as total_sold, " +
                   "SUM(oi.subtotal) as total_revenue FROM order_items oi " +
                   "JOIN items i ON oi.item_id = i.item_id " +
                   "JOIN orders o ON oi.order_id = o.order_id " +
                   "WHERE o.status = 'completed' " +
                   "GROUP BY i.item_id ORDER BY total_sold DESC LIMIT 10";
        String[] headers = {"Item Name", "Total Sold", "Total Revenue"};
        String[] columns = {"item_name", "total_sold", "total_revenue"};
        
        System.out.println("\n=== TOP 10 SELLING ITEMS ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void generateRevenueReport() {
        String sql = "SELECT strftime('%Y-%m', order_date) as month, " +
                   "COUNT(*) as total_orders, SUM(total_amount) as revenue " +
                   "FROM orders WHERE status = 'completed' " +
                   "GROUP BY strftime('%Y-%m', order_date) ORDER BY month DESC LIMIT 12";
        String[] headers = {"Month", "Total Orders", "Revenue"};
        String[] columns = {"month", "total_orders", "revenue"};
        
        System.out.println("\n=== MONTHLY REVENUE REPORT (Last 12 Months) ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void viewStaffPerformance() {
        String sql = "SELECT u.full_name, u.role, COUNT(o.order_id) as orders_processed, " +
                   "SUM(o.total_amount) as total_sales FROM users u " +
                   "LEFT JOIN orders o ON u.user_id = o.user_id " +
                   "WHERE u.role IN ('staff', 'manager') " +
                   "GROUP BY u.user_id ORDER BY orders_processed DESC";
        String[] headers = {"Staff Name", "Role", "Orders Processed", "Total Sales"};
        String[] columns = {"full_name", "role", "orders_processed", "total_sales"};
        
        System.out.println("\n=== STAFF PERFORMANCE REPORT ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    // ==================== SUPPLIER MENU ====================
    private static void showSupplierMenu() {
        System.out.println("\n=== SUPPLIER MANAGEMENT ===");
        System.out.println("1. View All Suppliers");
        System.out.println("2. Add Supplier");
        System.out.println("3. Update Supplier");
        System.out.println("4. Delete Supplier");
        System.out.println("5. Back to Dashboard");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                viewAllSuppliers();
                break;
            case 2:
                addSupplier();
                break;
            case 3:
                updateSupplier();
                break;
            case 4:
                deleteSupplier();
                break;
            case 5:
                return;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    private static void viewAllSuppliers() {
        String sql = "SELECT supplier_id, supplier_name, contact_person, email, phone, address, website " +
                   "FROM suppliers ORDER BY supplier_name";
        String[] headers = {"ID", "Supplier Name", "Contact Person", "Email", "Phone", "Address", "Website"};
        String[] columns = {"supplier_id", "supplier_name", "contact_person", "email", "phone", "address", "website"};
        
        System.out.println("\n=== ALL SUPPLIERS ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void addSupplier() {
        System.out.println("\n=== ADD NEW SUPPLIER ===");
        System.out.print("Supplier Name: ");
        String supplierName = scanner.nextLine();
        System.out.print("Contact Person: ");
        String contactPerson = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Phone: ");
        String phone = scanner.nextLine();
        System.out.print("Address: ");
        String address = scanner.nextLine();
        System.out.print("Website: ");
        String website = scanner.nextLine();
        
        String sql = "INSERT INTO suppliers (supplier_name, contact_person, email, phone, address, website) " +
                   "VALUES (?, ?, ?, ?, ?, ?)";
        conf.addRecord(sql, supplierName, contactPerson, email, phone, address, website);
        System.out.println("✓ Supplier added successfully!");
    }
    
    private static void updateSupplier() {
        viewAllSuppliers();
        System.out.print("\nEnter Supplier ID to update: ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("New Supplier Name: ");
        String supplierName = scanner.nextLine();
        System.out.print("New Contact Person: ");
        String contactPerson = scanner.nextLine();
        System.out.print("New Email: ");
        String email = scanner.nextLine();
        System.out.print("New Phone: ");
        String phone = scanner.nextLine();
        
        String sql = "UPDATE suppliers SET supplier_name = ?, contact_person = ?, email = ?, phone = ? WHERE supplier_id = ?";
        conf.updateRecord(sql, supplierName, contactPerson, email, phone, supplierId);
        System.out.println("✓ Supplier updated successfully!");
    }
    
    private static void deleteSupplier() {
        viewAllSuppliers();
        System.out.print("\nEnter Supplier ID to delete: ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();
        
        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";
        conf.deleteRecord(sql, supplierId);
        System.out.println("✓ Supplier deleted successfully!");
    }
    
    // ==================== INVENTORY MENU ====================
    private static void showInventoryMenu() {
        System.out.println("\n=== INVENTORY MANAGEMENT ===");
        System.out.println("1. View All Items");
        System.out.println("2. Add Item");
        System.out.println("3. Update Item");
        System.out.println("4. Delete Item");
        System.out.println("5. View Low Stock Items");
        System.out.println("6. Back to Dashboard");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                viewAllItems();
                break;
            case 2:
                addItem();
                break;
            case 3:
                updateItem();
                break;
            case 4:
                deleteItem();
                break;
            case 5:
                viewLowStockItems();
                break;
            case 6:
                return;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    private static void viewAllItems() {
        String sql = "SELECT i.item_id, i.item_name, i.item_type, i.category, i.quantity, i.unit, " +
                   "i.price, i.reorder_level, s.supplier_name, i.storage_location " +
                   "FROM items i " +
                   "LEFT JOIN suppliers s ON i.supplier_id = s.supplier_id " +
                   "ORDER BY i.item_name";
        String[] headers = {"ID", "Item Name", "Type", "Category", "Qty", "Unit", "Price", "Reorder", "Supplier", "Location"};
        String[] columns = {"item_id", "item_name", "item_type", "category", "quantity", "unit", "price", "reorder_level", "supplier_name", "storage_location"};
        
        System.out.println("\n=== ALL INVENTORY ITEMS ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void addItem() {
        System.out.println("\n=== ADD NEW ITEM ===");
        System.out.print("Item Name: ");
        String itemName = scanner.nextLine();
        System.out.print("Item Type (fabric/notion/tool): ");
        String itemType = scanner.nextLine();
        System.out.print("Category: ");
        String category = scanner.nextLine();
        System.out.print("Quantity: ");
        double quantity = scanner.nextDouble();
        scanner.nextLine();
        System.out.print("Unit: ");
        String unit = scanner.nextLine();
        System.out.print("Price: ");
        double price = scanner.nextDouble();
        System.out.print("Reorder Level: ");
        double reorderLevel = scanner.nextDouble();
        scanner.nextLine();
        
        viewAllSuppliers();
        System.out.print("Supplier ID (or 0 for none): ");
        int supplierId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("Storage Location: ");
        String location = scanner.nextLine();
        
        String sql = "INSERT INTO items (item_name, item_type, category, quantity, unit, price, reorder_level, supplier_id, storage_location) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        if (supplierId == 0) {
            conf.addRecord(sql, itemName, itemType, category, quantity, unit, price, reorderLevel, null, location);
        } else {
            conf.addRecord(sql, itemName, itemType, category, quantity, unit, price, reorderLevel, supplierId, location);
        }
        System.out.println("✓ Item added successfully!");
    }
    
    private static void updateItem() {
        viewAllItems();
        System.out.print("\nEnter Item ID to update: ");
        int itemId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.println("1. Update Quantity");
        System.out.println("2. Update Price");
        System.out.println("3. Update Full Details");
        System.out.print("Choose option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                System.out.print("New Quantity: ");
                double quantity = scanner.nextDouble();
                scanner.nextLine();
                String sql1 = "UPDATE items SET quantity = ? WHERE item_id = ?";
                conf.updateRecord(sql1, quantity, itemId);
                System.out.println("✓ Quantity updated successfully!");
                break;
            case 2:
                System.out.print("New Price: ");
                double price = scanner.nextDouble();
                scanner.nextLine();
                String sql2 = "UPDATE items SET price = ? WHERE item_id = ?";
                conf.updateRecord(sql2, price, itemId);
                System.out.println("✓ Price updated successfully!");
                break;
            case 3:
                System.out.print("New Item Name: ");
                String itemName = scanner.nextLine();
                System.out.print("New Quantity: ");
                double qty = scanner.nextDouble();
                System.out.print("New Price: ");
                double prc = scanner.nextDouble();
                scanner.nextLine();
                String sql3 = "UPDATE items SET item_name = ?, quantity = ?, price = ? WHERE item_id = ?";
                conf.updateRecord(sql3, itemName, qty, prc, itemId);
                System.out.println("✓ Item updated successfully!");
                break;
        }
    }
    
    private static void deleteItem() {
        viewAllItems();
        System.out.print("\nEnter Item ID to delete: ");
        int itemId = scanner.nextInt();
        scanner.nextLine();
        
        String sql = "DELETE FROM items WHERE item_id = ?";
        conf.deleteRecord(sql, itemId);
        System.out.println("✓ Item deleted successfully!");
    }
    
    private static void viewLowStockItems() {
        String sql = "SELECT item_id, item_name, quantity, unit, reorder_level " +
                   "FROM items WHERE quantity <= reorder_level ORDER BY quantity";
        String[] headers = {"ID", "Item Name", "Current Qty", "Unit", "Reorder Level"};
        String[] columns = {"item_id", "item_name", "quantity", "unit", "reorder_level"};
        
        System.out.println("\n=== LOW STOCK ITEMS (Need Reordering) ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    // ==================== ORDER MENU ====================
    private static void showOrderMenu() {
        System.out.println("\n=== ORDER MANAGEMENT ===");
        System.out.println("1. View All Orders");
        System.out.println("2. View Order Details");
        System.out.println("3. Create New Order");
        System.out.println("4. Update Order Status");
        System.out.println("5. Delete Order");
        System.out.println("6. Back to Dashboard");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                viewAllOrders();
                break;
            case 2:
                System.out.print("Enter Order ID: ");
                int orderId = scanner.nextInt();
                scanner.nextLine();
                viewOrderDetails(orderId);
                break;
            case 3:
                createOrder();
                break;
            case 4:
                updateOrderStatus();
                break;
            case 5:
                deleteOrder();
                break;
            case 6:
                return;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    private static void viewAllOrders() {
        String sql = "SELECT o.order_id, u.username, o.order_date, o.total_amount, o.status, o.payment_method " +
                   "FROM orders o " +
                   "JOIN users u ON o.user_id = u.user_id " +
                   "ORDER BY o.order_date DESC";
        String[] headers = {"Order ID", "Customer", "Order Date", "Total Amount", "Status", "Payment Method"};
        String[] columns = {"order_id", "username", "order_date", "total_amount", "status", "payment_method"};
        
        System.out.println("\n=== ALL ORDERS ===");
        conf.viewRecords(sql, headers, columns);
    }
    
    private static void viewOrderDetails(int orderId) {
        String sql = "SELECT oi.order_item_id, i.item_name, oi.quantity, oi.unit_price, oi.subtotal " +
                   "FROM order_items oi " +
                   "JOIN items i ON oi.item_id = i.item_id " +
                   "WHERE oi.order_id = ?";
        String[] headers = {"Item ID", "Item Name", "Quantity", "Unit Price", "Subtotal"};
        String[] columns = {"order_item_id", "item_name", "quantity", "unit_price", "subtotal"};
        
        System.out.println("\n=== ORDER DETAILS (Order #" + orderId + ") ===");
        
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, orderId);
            
            String headerSql = "SELECT o.order_id, u.full_name, o.order_date, o.total_amount, o.status, o.payment_method, o.notes " +
                             "FROM orders o JOIN users u ON o.user_id = u.user_id WHERE o.order_id = ?";
            PreparedStatement headerStmt = conn.prepareStatement(headerSql);
            headerStmt.setInt(1, orderId);
            ResultSet headerRs = headerStmt.executeQuery();
            
            if (headerRs.next()) {
                System.out.println("Customer: " + headerRs.getString("full_name"));
                System.out.println("Date: " + headerRs.getString("order_date"));
                System.out.println("Status: " + headerRs.getString("status"));
                System.out.println("Payment: " + headerRs.getString("payment_method"));
                System.out.println("Notes: " + headerRs.getString("notes"));
                System.out.println("Total: ₱" + headerRs.getDouble("total_amount"));
                System.out.println("\n--- Order Items ---");
            }
            
            headerRs.close();
            headerStmt.close();
            
        } catch (Exception e) {
            System.out.println("Error viewing order details: " + e.getMessage());
        }
        
        conf.viewRecords(sql, headers, columns, orderId);
    }
    
    private static void createOrder() {
        System.out.println("\n=== CREATE NEW ORDER ===");
        
        viewAllUsers();
        System.out.print("Enter User ID: ");
        int userId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("Payment Method (cash/card/gcash): ");
        String paymentMethod = scanner.nextLine();
        System.out.print("Notes: ");
        String notes = scanner.nextLine();
        
        String orderSql = "INSERT INTO orders (user_id, payment_method, notes, total_amount, status) " +
                        "VALUES (?, ?, ?, 0, 'pending')";
        conf.addRecord(orderSql, userId, paymentMethod, notes);
        
        int orderId = getLastInsertedOrderId();
        
        if (orderId > 0) {
            System.out.println("✓ Order created with ID: " + orderId);
            
            boolean addingItems = true;
            double totalAmount = 0;
            
            while (addingItems) {
                viewAllItems();
                System.out.print("\nEnter Item ID to add (0 to finish): ");
                int itemId = scanner.nextInt();
                
                if (itemId == 0) {
                    addingItems = false;
                    continue;
                }
                
                System.out.print("Enter Quantity: ");
                double quantity = scanner.nextDouble();
                scanner.nextLine();
                
                double price = getItemPrice(itemId);
                double subtotal = price * quantity;
                totalAmount += subtotal;
                
                String itemSql = "INSERT INTO order_items (order_id, item_id, quantity, unit_price, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?)";
                conf.addRecord(itemSql, orderId, itemId, quantity, price, subtotal);
                
                updateItemQuantity(itemId, quantity, false);
                
                System.out.println("✓ Item added to order! Subtotal: ₱" + subtotal);
            }
            
            String updateTotalSql = "UPDATE orders SET total_amount = ? WHERE order_id = ?";
            conf.updateRecord(updateTotalSql, totalAmount, orderId);
            
            System.out.println("\n✓ Order completed! Total: ₱" + totalAmount);
        } else {
            System.out.println("❌ Error creating order!");
        }
    }
    
    private static int getLastInsertedOrderId() {
        String sql = "SELECT MAX(order_id) as last_id FROM orders";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("last_id");
            }
        } catch (Exception e) {
            System.out.println("Error getting last order ID: " + e.getMessage());
        }
        return 0;
    }
    
    private static double getItemPrice(int itemId) {
        String sql = "SELECT price FROM items WHERE item_id = ?";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("price");
            }
        } catch (Exception e) {
            System.out.println("Error getting item price: " + e.getMessage());
        }
        return 0.0;
    }
    
    private static void updateItemQuantity(int itemId, double quantity, boolean isReturn) {
        String sql;
        if (isReturn) {
            sql = "UPDATE items SET quantity = quantity + ? WHERE item_id = ?";
        } else {
            sql = "UPDATE items SET quantity = quantity - ? WHERE item_id = ?";
        }
        conf.updateRecord(sql, quantity, itemId);
    }
    
    private static void updateOrderStatus() {
        viewAllOrders();
        System.out.print("\nEnter Order ID to update: ");
        int orderId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.println("1. Pending");
        System.out.println("2. Processing");
        System.out.println("3. Completed");
        System.out.println("4. Cancelled");
        System.out.print("Choose status: ");
        int statusChoice = scanner.nextInt();
        scanner.nextLine();
        
        String status = "";
        switch (statusChoice) {
            case 1: status = "pending"; break;
            case 2: status = "processing"; break;
            case 3: status = "completed"; break;
            case 4: status = "cancelled"; break;
            default:
                System.out.println("Invalid status!");
                return;
        }
        
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        conf.updateRecord(sql, status, orderId);
        System.out.println("✓ Order status updated to: " + status);
    }
    
    private static void deleteOrder() {
        viewAllOrders();
        System.out.print("\nEnter Order ID to delete: ");
        int orderId = scanner.nextInt();
        scanner.nextLine();
        
        System.out.print("Are you sure? This will delete all order items. (y/n): ");
        String confirm = scanner.nextLine();
        
        if (confirm.equalsIgnoreCase("y")) {
            String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
            conf.deleteRecord(deleteItemsSql, orderId);
            
            String deleteOrderSql = "DELETE FROM orders WHERE order_id = ?";
            conf.deleteRecord(deleteOrderSql, orderId);
            
            System.out.println("✓ Order deleted successfully!");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }
    
    // ==================== USER MENU ====================
    private static void showUserMenu() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║         USER DASHBOARD               ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("Welcome, " + currentUser.fullName);
        System.out.println("\n1. View Available Items");
        System.out.println("2. My Orders");
        System.out.println("3. Place Order");
        System.out.println("4. My Profile");
        System.out.println("5. Logout");
        System.out.print("Choose option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1:
                viewAllItems();
                break;
            case 2:
                viewMyOrders();
                break;
            case 3:
                placeUserOrder();
                break;
            case 4:
                showUserProfile();
                break;
            case 5:
                currentUser = null;
                System.out.println("✓ Logged out successfully!");
                break;
            default:
                System.out.println("Invalid option!");
        }
    }
    
    private static void viewMyOrders() {
        String sql = "SELECT order_id, order_date, total_amount, status, payment_method " +
                   "FROM orders WHERE user_id = ? ORDER BY order_date DESC";
        String[] headers = {"Order ID", "Order Date", "Total Amount", "Status", "Payment Method"};
        String[] columns = {"order_id", "order_date", "total_amount", "status", "payment_method"};
        
        System.out.println("\n=== MY ORDERS ===");
        
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, currentUser.userId);
            
            ResultSet rs = pstmt.executeQuery();
            boolean hasOrders = false;
            
            System.out.println(String.format("%-10s %-20s %-15s %-15s %-20s", 
                headers[0], headers[1], headers[2], headers[3], headers[4]));
            System.out.println("================================================================================");
            
            while (rs.next()) {
                hasOrders = true;
                System.out.println(String.format("%-10d %-20s P%-14.2f %-15s %-20s",
                    rs.getInt("order_id"),
                    rs.getString("order_date"),
                    rs.getDouble("total_amount"),
                    rs.getString("status"),
                    rs.getString("payment_method")
                ));
            }
            
            if (!hasOrders) {
                System.out.println("No orders found.");
            }
            
        } catch (Exception e) {
            System.out.println("Error viewing orders: " + e.getMessage());
        }
    }
    
    private static void placeUserOrder() {
        System.out.println("\n=== PLACE NEW ORDER ===");
        System.out.print("Payment Method (cash/card/gcash): ");
        String paymentMethod = scanner.nextLine();
        System.out.print("Notes/Special Instructions: ");
        String notes = scanner.nextLine();
        
        String orderSql = "INSERT INTO orders (user_id, payment_method, notes, total_amount, status) " +
                        "VALUES (?, ?, ?, 0, 'pending')";
        conf.addRecord(orderSql, currentUser.userId, paymentMethod, notes);
        
        int orderId = getLastInsertedOrderId();
        
        if (orderId > 0) {
            System.out.println("✓ Order created with ID: " + orderId);
            
            boolean addingItems = true;
            double totalAmount = 0;
            
            while (addingItems) {
                viewAllItems();
                System.out.print("\nEnter Item ID to add (0 to finish): ");
                int itemId = scanner.nextInt();
                
                if (itemId == 0) {
                    addingItems = false;
                    continue;
                }
                
                double availableQty = getItemQuantity(itemId);
                System.out.println("Available quantity: " + availableQty);
                
                System.out.print("Enter Quantity: ");
                double quantity = scanner.nextDouble();
                scanner.nextLine();
                
                if (quantity > availableQty) {
                    System.out.println("❌ Not enough stock! Available: " + availableQty);
                    continue;
                }
                
                double price = getItemPrice(itemId);
                double subtotal = price * quantity;
                totalAmount += subtotal;
                
                String itemSql = "INSERT INTO order_items (order_id, item_id, quantity, unit_price, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?)";
                conf.addRecord(itemSql, orderId, itemId, quantity, price, subtotal);
                
                updateItemQuantity(itemId, quantity, false);
                
                System.out.println("✓ Item added to order! Subtotal: ₱" + subtotal);
            }
            
            if (totalAmount > 0) {
                String updateTotalSql = "UPDATE orders SET total_amount = ? WHERE order_id = ?";
                conf.updateRecord(updateTotalSql, totalAmount, orderId);
                
                System.out.println("\n✓ Order placed successfully!");
                System.out.println("Order ID: " + orderId);
                System.out.println("Total Amount: ₱" + totalAmount);
                System.out.println("Status: Pending approval");
            } else {
                String deleteSql = "DELETE FROM orders WHERE order_id = ?";
                conf.deleteRecord(deleteSql, orderId);
                System.out.println("Order cancelled - no items added.");
            }
        } else {
            System.out.println("❌ Error creating order!");
        }
    }
    
    private static double getItemQuantity(int itemId) {
        String sql = "SELECT quantity FROM items WHERE item_id = ?";
        try (Connection conn = conf.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("quantity");
            }
        } catch (Exception e) {
            System.out.println("Error getting item quantity: " + e.getMessage());
        }
        return 0.0;
    }
    
    private static void showUserProfile() {
        System.out.println("\n=== MY PROFILE ===");
        System.out.println("Username: " + currentUser.username);
        System.out.println("Full Name: " + currentUser.fullName);
        System.out.println("Email: " + currentUser.email);
        System.out.println("Phone: " + currentUser.phone);
        System.out.println("Address: " + currentUser.address);
        System.out.println("Status: " + currentUser.status);
    }
}