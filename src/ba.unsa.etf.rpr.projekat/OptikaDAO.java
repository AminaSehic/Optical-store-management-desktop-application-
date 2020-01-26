package ba.unsa.etf.rpr.projekat;


import ba.unsa.etf.rpr.projekat.Exceptions.InvalidEmployeeDataException;
import ba.unsa.etf.rpr.projekat.Models.Employee;
import ba.unsa.etf.rpr.projekat.Models.Glasses;
import ba.unsa.etf.rpr.projekat.Models.Shop;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class OptikaDAO {
    private static int idEmployee, idShop, idGlasses;
    private static OptikaDAO instance;
    private Connection conn = null;
    private PreparedStatement statement;

    //    public static OptikaDAO getInstance() {
//
//        if (instance == null) instance = new OptikaDAO();
//        return instance;
//    }
    public OptikaDAO() {
        getIndexes();
    }

    private void getIndexes() {

        start("Select max(id) from employee");
        try {
            ResultSet rs = statement.executeQuery();
            idEmployee = rs.getInt(1);
            close();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        start("Select max(id) from shop");
        try {
            ResultSet rs = statement.executeQuery();
            idShop = rs.getInt(1);
            close();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        start("Select max(id) from glasses");
        try {
            ResultSet rs = statement.executeQuery();
            idGlasses = rs.getInt(1);
            close();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
    }

    public void prepareStatement(String s) {
        try {
            statement = conn.prepareStatement(s);
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
    }

    public void start(String s) {
        try {
            String url = "jdbc:sqlite:optika.db";
            conn = DriverManager.getConnection(url);
            PreparedStatement testStatement = conn.prepareStatement("SELECT * FROM employee, glasses, shop");
            testStatement.executeQuery();
        } catch (SQLException e) {
            regenerisiBazu();
            setDatabase();
        }
        prepareStatement(s);
    }


    public Employee dajZaposlenika(String name, String password) {
        Employee employee = new Employee();
        try {
            start("select * from employee where name=? and password_hash=?");
            statement.setString(1, name);
            statement.setString(2, dajPasswordHash(password));
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                employee.setId(rs.getInt("id"));
                employee.setName(rs.getString("name"));
                employee.setName(rs.getString("name"));

                employee.setLastName(rs.getString("last_name"));
                employee.setBirthDate(rs.getString("birth_date"));

                employee.setAddress(rs.getString("address"));
                employee.setContactNumber(rs.getString("phone_number"));

                employee.setType(parseEmployeeType(rs.getString("type")));
                employee.setPassword_hash(rs.getString("password_hash"));

                employee.setShop(dajRadnju(rs.getInt("shop_id")));
            }

        } catch (SQLException | NoSuchAlgorithmException | InvalidEmployeeDataException e) {
            e.printStackTrace();
            close();
        }
        close();
        return employee;
    }

    private Employee.Type parseEmployeeType(String v) throws InvalidEmployeeDataException {
        if (Employee.Type.ADMIN.toString().toLowerCase().equals(v.toLowerCase())) {
            return Employee.Type.ADMIN;
        } else if (Employee.Type.OWNER.toString().toLowerCase().equals(v.toLowerCase())) {
            return Employee.Type.OWNER;
        } else if (Employee.Type.EMPLOYEE.toString().toLowerCase().equals(v.toLowerCase())) {
            return Employee.Type.EMPLOYEE;
        } else {
            throw new InvalidEmployeeDataException("Nepostojeci tip zaposlenika");
        }

    }

    public ArrayList<Employee> dajSveZaposlenike() {
        ArrayList<Employee> zaposlenici = new ArrayList<>();
        try {
            start("select * from employee");
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return zaposlenici;
            }
            do {
                Employee e = new Employee(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("birth_date"),
                        rs.getString("address"),
                        rs.getString("phone_number"),
                        parseEmployeeType(rs.getString("type")),
                        rs.getString("password_hash"),
                        dajRadnju(rs.getInt("shop_id")));
                zaposlenici.add(e);
            } while (rs.next());

        } catch (SQLException | InvalidEmployeeDataException e) {
            e.printStackTrace();
            close();
        }
        close();
        return zaposlenici;
    }

    public Shop dajRadnju(int id) {
        Shop s = null;
        try {
            start("select * from shop where id=?");
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) return null;
            s = new Shop(rs.getInt("id"), rs.getString("shop_name"), rs.getString("address"));

        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        close();
        return s;
    }

    public ArrayList<Glasses> dajNaocaleIzRadnje(int id_radnje) {
        ArrayList<Glasses> naocale = new ArrayList<>();
        try {
            start("select * from glasses where shop_id=?");
            statement.setInt(1, id_radnje);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Glasses g = new Glasses(rs.getInt("id"), rs.getString("manufacturer"),
                        rs.getString("model"), rs.getInt("yearOfProduction"),
                        rs.getInt("type"), rs.getInt("price"), dajRadnju(rs.getInt("shop_id")),
                        rs.getInt("number"));
                naocale.add(g);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        close();
        return naocale;
    }

    public void dodajZaposlenika(Employee e) {
        try {
            start("insert into employee values(?,?,?,?,?,?,?,?,?)");
            //int id, String name, String lastName, String birthDate, String address, String contactNumber, int type, String password, int shop
            statement.setInt(1, getNewEmployeeId());
            statement.setString(2, e.getName());
            statement.setString(3, e.getLastName());
            statement.setString(4, e.getBirthDate());
            statement.setString(5, e.getAddress());
            statement.setString(6, e.getContactNumber());
            statement.setString(7, e.getPassword_hash());
            statement.setString(8, e.getType().toString());
            statement.setInt(9, e.getShop().getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            close();
        }
        close();
    }

    public void prodajNaocale(int id) {
        try {
            start("update glasses set number=(number-1) where id=?");
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        close();
    }

    public void dodajRadnju(Shop s) {
        try {
            start("insert into shop values(?,?,?)");
            statement.setInt(1, getNewShopId());
            statement.setString(2, s.getShopName());
            statement.setString(3, s.getAddress());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        close();
    }

    public void deleteEmployee(Employee employee) {
        try {
            start("DELETE from Employee where id=?");
            statement.setInt(1, employee.getId());
            statement.execute();
            close();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
    }

    private static byte[] getSHA(String input) throws NoSuchAlgorithmException {
        // Static getInstance method is called with hashing SHA
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // digest() method called
        // to calculate message digest of an input
        // and return array of byte
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String dajPasswordHash(String password) throws NoSuchAlgorithmException {
        byte[] hash = getSHA(password);
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }


    private void regenerisiBazu() {
        Scanner ulaz = null;
        try {
            ulaz = new Scanner(new FileInputStream("resources/database/optika.db.sql"));
            String sqlUpit = "";
            while (ulaz.hasNext()) {
                sqlUpit += ulaz.nextLine();
                if (sqlUpit.charAt(sqlUpit.length() - 1) == ';') {
                    try {
                        Statement stmt = conn.createStatement();
                        stmt.execute(sqlUpit);
                        sqlUpit = "";
                    } catch (SQLException e) {
                        e.printStackTrace();
                        close();
                    }
                }
            }
            ulaz.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setDatabase() {
        start("insert or replace into shop values(?,?,?)");
        try {
            statement.setInt(1, 1);
            statement.setString(2, "Oko");
            statement.setString(3, "Trg Alije Izetbegovica bb");
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        start("insert or replace into employee values(?,?,?,?,?,?,?,?,?)");
        try {
            statement.setInt(1, 1);
            statement.setString(2, "admin");
            statement.setString(3, "admin");
            statement.setString(4, "1/1/1970");
            statement.setString(5, "Zmaja od Bosne bb");
            statement.setString(6, "911");
            statement.setString(7, dajPasswordHash("admin"));
            statement.setString(8, "admin");
            statement.setInt(9, 1);
            statement.executeUpdate();
            close();
        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            close();
        }

    }

    private int getNewEmployeeId() {
        return ++idEmployee;
    }

    private int getNewShopId() {
        return ++idShop;
    }

    private int getNewGlassesId() {
        return ++idGlasses;
    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
    }


    public ArrayList<Shop> getAllShops() {
        ArrayList<Shop> shops = new ArrayList<>();
        start("Select * from shop");
        try {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Shop shop = new Shop(rs.getInt("id"), rs.getString("shop_name"), rs.getString("address"));
                shops.add(shop);
            }
            close();
        } catch (SQLException e) {
            e.printStackTrace();
            close();
        }
        close();
        return shops;
    }
}