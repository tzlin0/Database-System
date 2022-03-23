package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;

public class Caregiver {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Caregiver(CaregiverBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Caregiver(CaregiverGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addCaregiver = "INSERT INTO Caregivers VALUES (? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addCaregiver);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void uploadAvailability(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setDate(1, d);
            statement.setString(2, this.username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
    public void showAppointments() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String showapt = "SELECT id, p_name, c_name, Time,v_name FROM Appointments WHERE c_name = ?;";
        try {
            PreparedStatement statement = con.prepareStatement(showapt);
            statement.setString(1, this.username);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()){
                System.out.println("There is currently no appointments ");
            }
            else{
                System.out.println("Appointments: ");
                while(resultSet.next()){
                    String aptID = resultSet.getString("id");
                    String p_name = resultSet.getString("p_name");
                    Date Time = resultSet.getDate("Time");
                    String v_name = resultSet.getString("v_name");
                    System.out.println("Appointment ID: "+aptID+" vaccine name: "+v_name+" date: "+Time+" patient name: "+p_name);
                }
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }
    public void removeAvailability(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String rmAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(rmAvailability);
            statement.setDate(1, d);
            statement.setString(2, this.username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class CaregiverBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public CaregiverBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Caregiver build() {
            return new Caregiver(this);
        }
    }

    public static class CaregiverGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public CaregiverGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Caregiver get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getCaregiver = "SELECT Salt, Hash FROM Caregivers WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getCaregiver);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Caregiver(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
