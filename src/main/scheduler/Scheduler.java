package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.UUID;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        // do we need to log in to search, or do we have to even be P or C? Yes, both p and c ;;/.///';/
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        // do we have to be a P to reserve? yes
        System.out.println("> upload_availability <date>");// has to be a C
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        // can we cancel if we are C or only P
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        // do we have to log in? do we have to be a P or C. only show appointments of yourself
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        //
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        // distinct names
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!checkPW(password)){// pw not valid
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        // distinct names 
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!checkPW(password)){// pw not valid
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
//            while (resultSet.next()){
//
//            }
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }


    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // Check 1: the length for tokens need to be exactly 2 to include the date format.
        if (tokens.length !=2){
            System.out.println("Please try again");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            String searchCaregiverSchedule = " SELECT Username FROM Availabilities WHERE Time = ?; ";
            PreparedStatement statement = con.prepareStatement(searchCaregiverSchedule);
            statement.setDate(1,d);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()){
                System.out.println("There is currently no schedule on "+ date);
            }
            else{
                System.out.println("The available Caregivers list on "+ date);
                while(resultSet.next()){
                    System.out.println(resultSet.getString("UserName"));
                }
            }
            String searchvaccine = "SELECT Name,Doses FROM Vaccines;";
            PreparedStatement stmt = con.prepareStatement(searchvaccine);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                System.out.println("Vaccine list and numbers: ");
                System.out.println(rs.getString("Name")+" has "+rs.getInt("Doses")+" doses left.");
            }
        } catch (SQLException e) {
            System.out.println("SQL error. ");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // check if the user is logged in as a patient
        if (currentPatient==null){
            System.out.println("Please login as a patient first, thank you!");
        }
        if (tokens.length !=3){
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];
        String vaccineName = tokens[2];
        String caregiver;

        // check caregiver availability
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            Date d1 = Date.valueOf(date);
            String searchCaregiverSchedule = " SELECT Username FROM Availabilities WHERE Time = ?; ";
            PreparedStatement statement = con.prepareStatement(searchCaregiverSchedule);
            statement.setDate(1, d1);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("There is currently no available Caregiver on " + date);
                return;
            }
            // available, caregiver exists
            resultSet.next();
            caregiver = resultSet.getString("UserName");

            // set vaccine
            Vaccine vaccine = null;
            try {
                vaccine = new Vaccine.VaccineGetter(vaccineName).get();

            } catch (SQLException e) {
                System.out.println("Error occurred when getting doses");
                e.printStackTrace();
            }
            // check if vaccine exists;
            // if not exist
            if (vaccine == null){
                System.out.println("Sorry, we don't have the "+vaccineName+" brand.");
                System.out.println("Please try a different vaccine brand");
                return;
            }
            else if (vaccine.getAvailableDoses()==0){
                System.out.println("Sorry, we have no more "+vaccineName);
                System.out.println("Please try a different vaccine brand");
                return;
            }
            // create the row!!
            else {
                String makeappointment = "INSERT INTO Appointments VALUES (?,?,?,?,?);";
                try {
                    UUID uuid = UUID.randomUUID();
                    String id = uuid.toString();
                    PreparedStatement statement1 = con.prepareStatement(makeappointment);
                    statement1.setString(1, id);
                    statement1.setString(2, currentPatient.getUsername());
                    statement1.setString(3, caregiver);
                    statement1.setDate(4, d1);
                    statement1.setString(5, vaccineName);
                    statement1.executeUpdate();
                    System.out.println("Successfully Scheduled your Appointment.");
                    System.out.println("Caregiver: "+caregiver+", Appointment id: "+id);
                    // remove availability
                    currentPatient.removeAvailability(d1,caregiver);
                    // vaccine dose -1
                    vaccine.decreasoneDose();
                } catch (SQLException e) {
                    System.out.println("Error during creating appointment row");
                    throw new SQLException();
                }
            }
        }catch (SQLException e) {
                System.out.println("SQL error first part");
                e.printStackTrace();
            }



//        Appointment appointment = new Appointment.AppointmentBuilder(p_name,c_name);
    }
    private static boolean checkPW(String str) {
        char ch;
        boolean capitalFlag = false;
        boolean lowerCaseFlag = false;
        boolean numberFlag = false;
        boolean SpecialFlag = false;
        for(int i=0;i < str.length();i++) {
            ch = str.charAt(i);
            if( Character.isDigit(ch)) {
                numberFlag = true;
            }
            else if (Character.isUpperCase(ch)) {
                capitalFlag = true;
            } else if (Character.isLowerCase(ch)) {
                lowerCaseFlag = true;
            }
            else if(ch=='!'||ch=='@'||ch=='#'||ch=='?'){
                SpecialFlag = true;
            }
            if(numberFlag && capitalFlag && lowerCaseFlag&&i>=7&&SpecialFlag)
                return true;
        }
        System.out.println("Password not valid!");
        System.out.println("Password should be at least 8 characters, a mixture of both uppercase and lowercase letters,a mixture of letters and numbers,and one special character: !, @, #, ?");
        return false;
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];

        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // let's do some extra credits
        if (currentPatient==null&&currentCaregiver==null){
            System.out.println("Please login first, thank you!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String id = tokens[1];
        if (!appointmentIDExists(id)){ //mo id or other ppl's id
            System.out.println("Invalid appointment ID, please try again.");
            return;
        }
        else if(!isMyappointment(id)){
            System.out.println("Invalid appointment ID , please try again.");
            return;
        }
        else {
            cancelAndReverse(id);
            System.out.println("Appointment canceled!");
        }
    }
    private static boolean isMyappointment(String id) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT p_name, c_name FROM Appointments WHERE id = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            // check if p_name or c_name is the current name
            if (currentCaregiver!=null){
                String c_name = resultSet.getString("c_name");
                return c_name.equals(currentCaregiver.getUsername());
            }
            else if(currentPatient!=null){
                String p_name = resultSet.getString("p_name");
                return p_name.equals(currentPatient.getUsername());
            }
            else{
                System.out.println("Please log in");
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return false;
    }
    private static void cancelAndReverse(String id) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT v_name, Time, c_name FROM Appointments WHERE id = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();

            String c_name = resultSet.getString("c_name");
            String v_name = resultSet.getString("v_name");
            Date Time = resultSet.getDate("Time");
            // delete appointment
            String rmAppointment = "DELETE FROM Appointments WHERE id =?;";
            PreparedStatement statement1 = con.prepareStatement(rmAppointment);
            statement1.setString(1,id);
            statement1.executeUpdate();
            // add availability
            String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
            PreparedStatement statement2 = con.prepareStatement(addAvailability);
            statement2.setDate(1,Time);
            statement2.setString(2,c_name);
            statement2.executeUpdate();
            // add vaccine one dose
            Vaccine vaccine = null;
            try {
                vaccine = new Vaccine.VaccineGetter(v_name).get();
            } catch (SQLException e) {
                System.out.println("Error occurred when getting doses");
                e.printStackTrace();
            }
            if (vaccine == null) {
                try {
                    vaccine = new Vaccine.VaccineBuilder(v_name, 1).build();
                    vaccine.saveToDB();
                } catch (SQLException e) {
                    System.out.println("Error occurred when adding doses");
                    e.printStackTrace();
                }
            } else {
                // if the vaccine is not null, meaning that the vaccine already exists in our table
                try {
                    vaccine.increaseAvailableDoses(1);
                } catch (SQLException e) {
                    System.out.println("Error occurred when adding doses");
                    e.printStackTrace();
                }
            }


        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }
    private static boolean appointmentIDExists(String id) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Appointments WHERE id = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // check if we are logged in
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please logged-in first");
            return;
        }
        if (currentPatient != null) {
            try{
                currentPatient.showAppointments();
            }catch (SQLException e){
                System.out.println("Error occurred when showing appointment");
                e.printStackTrace();
            }
        }
        else{
            try{
                currentCaregiver.showAppointments();
            }catch (SQLException e){
                System.out.println("Error occurred when showing appointment");
                e.printStackTrace();
            }
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        try {
            currentCaregiver = null;
            currentPatient = null;
        } catch (Exception e){
            System.out.println("Error occurred when logging out");

        }
        System.out.println("Successfully logged out! ");
    }
}
