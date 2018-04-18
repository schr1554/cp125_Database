package com.scg.persistent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.scg.domain.ClientAccount;
import com.scg.domain.Consultant;
import com.scg.domain.Invoice;
import com.scg.domain.InvoiceLineItem;
import com.scg.domain.Skill;
import com.scg.domain.TimeCard;
import com.scg.util.Address;
import com.scg.util.DateRange;
import com.scg.util.Name;
import com.scg.util.StateCode;

/**
 * Responsible for providing a programmatic interface to store and access
 * objects in the database.
 * 
 * @author chq-alexs
 *
 */
public final class DbServer {

	/**
	 * Database URL
	 */
	private final String dbUrl;

	/**
	 * Database Username
	 */
	private final String username;

	/**
	 * Database password
	 */
	private final String password;

	/**
	 * Constructor.
	 * 
	 * @param dbUrl
	 *            - the database URL
	 * @param username
	 *            - the database username
	 * @param password
	 *            - the database password
	 */
	public DbServer(String dbUrl, String username, String password) {
		this.dbUrl = dbUrl;
		this.username = username;
		this.password = password;
	}

	/**
	 * Add a client to the database.
	 * 
	 * @param client
	 *            - the client to add
	 * 
	 * @throws SQLException
	 *             - if any database operations fail
	 */
	public void addClient(ClientAccount client) throws SQLException {
		Connection addClientConnection = connectionManager();

		String query = "INSERT INTO clients (name, street, city, state, postal_code, contact_last_name, contact_first_name, contact_middle_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

		PreparedStatement ps = addClientConnection.prepareStatement(query);
		ps.setString(1, client.getName());
		ps.setString(2, client.getAddress().getStreetNumber());
		ps.setString(3, client.getAddress().getCity());
		ps.setString(4, client.getAddress().getState().name());
		ps.setString(5, client.getAddress().getPostalCode());
		ps.setString(6, client.getContact().getLastName());
		ps.setString(7, client.getContact().getFirstName());
		ps.setString(8, client.getContact().getMiddleName());
		ps.executeUpdate();
		ps.close();
		addClientConnection.close();

	}

	/**
	 * Add a consultant to the database.
	 * 
	 * @param consultant
	 *            - the consultant to add
	 * @throws SQLException
	 *             - if any database operations fail
	 */
	public void addConsultant(Consultant consultant) throws SQLException {
		Connection addConsultantConnection = connectionManager();

		String query = "INSERT INTO CONSULTANTS (LAST_NAME, FIRST_NAME, MIDDLE_NAME) VALUES (?,?,?)";
		PreparedStatement ps = addConsultantConnection.prepareStatement(query);
		ps.setString(1, consultant.getName().getLastName());
		ps.setString(2, consultant.getName().getFirstName());
		ps.setString(3, consultant.getName().getMiddleName());
		ps.executeUpdate();
		ps.close();
		addConsultantConnection.close();

	}

	/**
	 * Add a timecard to the database.
	 * 
	 * @param timeCard
	 *            - the timecard to add
	 * @throws SQLException
	 *             - if any database operations fail
	 */
	public void addTimeCard(TimeCard timeCard) throws SQLException {
		Connection addTimeCardConnection = connectionManager();

		String getConsultantID = "SELECT id   FROM consultants  WHERE last_name = ?    AND first_name = ?    AND middle_name = ?";

		try {
			PreparedStatement ps = addTimeCardConnection.prepareStatement(getConsultantID);
			ps.setString(1, timeCard.getConsultant().getName().getLastName());
			ps.setString(2, timeCard.getConsultant().getName().getFirstName());
			ps.setString(3, timeCard.getConsultant().getName().getMiddleName());
			ResultSet rs = ps.executeQuery();
			String consultantID = null;

			if (rs.next()) {

				consultantID = rs.getString("id");

			}

			String insertTimeCardStatement = "INSERT INTO TIMECARDS	(CONSULTANT_ID, START_DATE) VALUES(?,?)";

			ps = addTimeCardConnection.prepareStatement(insertTimeCardStatement);
			ps.setString(1, consultantID);
			ps.setString(2, timeCard.getWeekStartingDay().toString());
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		timeCard.getConsultingHours().forEach(items -> {

			PreparedStatement ps;

			try {

				if (items.isBillable()) {
					String insertBillableHours = "INSERT INTO billable_hours (client_id, timecard_id, date, skill, hours) VALUES ((SELECT DISTINCT ID FROM CLIENTS WHERE NAME = ?), (SELECT DISTINCT ID FROM TIMECARDS WHERE START_DATE = ? AND CONSULTANT_ID = (SELECT id   FROM consultants  WHERE last_name = ?    AND first_name = ?    AND middle_name = ?)), ?, ?, ?)";
					ps = addTimeCardConnection.prepareStatement(insertBillableHours);

					ps.setString(1, items.getAccount().getName().toString());
					ps.setString(2, timeCard.getWeekStartingDay().toString());
					ps.setString(3, timeCard.getConsultant().getName().getLastName());
					ps.setString(4, timeCard.getConsultant().getName().getFirstName());
					ps.setString(5, timeCard.getConsultant().getName().getMiddleName());
					ps.setString(6, items.getDate().toString());
					ps.setString(7, items.getSkill().name());
					ps.setString(8, String.valueOf(items.getHours()));
					ps.executeUpdate();

				} else {
					String insertNonBillableHours = "INSERT INTO non_billable_hours (account_name, timecard_id, date, hours) VALUES (?, (SELECT DISTINCT ID FROM TIMECARDS WHERE START_DATE = ? AND CONSULTANT_ID = (SELECT id   FROM consultants  WHERE last_name = ?    AND first_name = ?    AND middle_name = ?)), ?,?)";

					ps = addTimeCardConnection.prepareStatement(insertNonBillableHours);
					String nonBillableAccount = null;

					if (items.getAccount().getName().equals("Business Development")) {
						nonBillableAccount = "BUSINESS_DEVELOPMENT";
					}

					if (items.getAccount().getName().equals("Sick Leave")) {
						nonBillableAccount = "SICK_LEAVE";
					}

					if (items.getAccount().getName().equals("Vacation")) {
						nonBillableAccount = "VACATION";
					}

					ps.setString(1, nonBillableAccount);
					ps.setString(2, timeCard.getWeekStartingDay().toString());
					ps.setString(3, timeCard.getConsultant().getName().getLastName());
					ps.setString(4, timeCard.getConsultant().getName().getFirstName());
					ps.setString(5, timeCard.getConsultant().getName().getMiddleName());
					ps.setString(6, items.getDate().toString());
					ps.setString(7, String.valueOf(items.getHours()));
					ps.executeUpdate();

				}

				ps.close();

			} catch (SQLException e) {
				e.printStackTrace();
			}

		});
		addTimeCardConnection.close();
	}

	/**
	 * Get all of the clients in the database.
	 * 
	 * @return a list of all of the clients
	 * 
	 * @throws SQLException
	 *             - if any database operations fail
	 */
	public List<ClientAccount> getClients() throws SQLException {

		Connection getClientsConnection = connectionManager();

		List<ClientAccount> clientList = new ArrayList<ClientAccount>();

		String getClients = "SELECT name, street, city, state, postal_code, contact_last_name, contact_first_name, contact_middle_name FROM clients";
		try {
			Statement stmt = getClientsConnection.createStatement();
			ResultSet rs = stmt.executeQuery(getClients);

			while (rs.next()) {

				String name = rs.getString("name");

				String streetNumber = rs.getString("street");
				String city = rs.getString("city");
				String stateCodeString = rs.getString("state");
				String postalCode = rs.getString("postal_code");

				String lastName = rs.getString("contact_last_name");
				String firstName = rs.getString("contact_first_name");
				String middleName = rs.getString("contact_middle_name");

				Name contact = new Name();

				if (!middleName.equals("null")) {
					contact.setFirstName(firstName);
					contact.setLastName(lastName);
					;
					contact.setMiddleName(middleName);
					;
				} else {
					contact.setFirstName(firstName);
					contact.setLastName(lastName);

				}

				Address address = new Address(streetNumber, city, StateCode.valueOf(stateCodeString), postalCode);

				clientList.add(new ClientAccount(name, contact, address));

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return clientList;

	}

	/**
	 * Get all of the consultant in the database.
	 * 
	 * @return a list of all of the consultants
	 * 
	 * @throws SQLException
	 *             - if any database operations fail
	 */
	public List<Consultant> getConsultants() throws SQLException {
		Connection getConsultantsConnection = connectionManager();

		List<Consultant> consultantList = new ArrayList<Consultant>();

		String getConsultant = "SELECT last_name, first_name, middle_name   FROM consultants";
		try {

			Statement stmt = getConsultantsConnection.createStatement();
			ResultSet rs = stmt.executeQuery(getConsultant);

			while (rs.next()) {
				String lastName = rs.getString("name");
				String firstName = rs.getString("name");
				String middleName = rs.getString("name");
				Name contact = new Name();

				if (!middleName.equals("null")) {
					contact.setFirstName(firstName);
					contact.setLastName(lastName);
					;
					contact.setMiddleName(middleName);
					;
				} else {
					contact.setFirstName(firstName);
					contact.setLastName(lastName);

				}

				Consultant consultant = new Consultant(contact);

				consultantList.add(consultant);

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return consultantList;

	}

	/**
	 * Get clients monthly invoice.
	 * 
	 * @param client
	 *            - the client to obtain the invoice line items for
	 * @param month
	 *            - the month of the invoice
	 * @param year
	 *            - the year of the invoice
	 * @return the clients invoice for the month
	 * @throws SQLException
	 *             - if any database operations fail
	 */
	public Invoice getInvoice(ClientAccount client, java.time.Month month, int year) throws SQLException {

		Connection getInvoiceConnection = connectionManager();

		Invoice invoice = new Invoice(client, month, year);

		String getInvoice = "SELECT b.date, c.last_name, c.first_name, c.middle_name, b.skill, s.rate, b.hours FROM billable_hours b, consultants c, skills s, timecards t  WHERE b.client_id = (SELECT DISTINCT id FROM clients WHERE name = ?) AND b.skill = s.name AND b.timecard_id = t.id AND c.id = t.consultant_id    AND b.date >= ?    AND b.date <= ?";
		try {
			PreparedStatement ps = getInvoiceConnection.prepareStatement(getInvoice);

			DateRange dateRange = new DateRange(month, year);

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-d");

			ps.setString(1, client.getName());
			ps.setString(2, dateRange.getStartDate().format(formatter));
			ps.setString(3, dateRange.getEndDate().format(formatter));

			java.sql.ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				String dateString = rs.getString("date");
				String lastName = rs.getString("last_name");
				String firstName = rs.getString("first_name");
				String middleName = rs.getString("middle_name");
				String skill = rs.getString("skill");
				// String rateString = rs.getString("rate");
				String hours = rs.getString("hours");

				Name contact = new Name();

				if (!middleName.equals("null")) {
					contact.setFirstName(firstName);
					contact.setLastName(lastName);
					contact.setMiddleName(middleName);
				} else {
					contact.setFirstName(firstName);
					contact.setLastName(lastName);

				}

				LocalDate localDate = LocalDate.parse(dateString, formatter);

				Consultant consultant = new Consultant(contact);

				InvoiceLineItem lineItem = new InvoiceLineItem(localDate, consultant, Skill.valueOf(skill),
						Integer.valueOf(hours));

				invoice.addLineItem(lineItem);

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return invoice;
	}

	/**
	 * Manages connection.
	 * 
	 * @return Database Connection
	 * @throws SQLException
	 */
	private Connection connectionManager() throws SQLException {

		Connection conn = null;
		String db = this.dbUrl;
		String driverClassName = "org.apache.derby.jdbc.ClientDriver";
		String user = this.username;
		String pass = this.password;
		try {
			Class.forName(driverClassName);
			conn = DriverManager.getConnection(db, user, pass);
		} catch (Exception ex) {
			ex.printStackTrace();

		}
		return conn;

	}

}
