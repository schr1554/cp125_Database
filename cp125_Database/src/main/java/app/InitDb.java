package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.scg.domain.ClientAccount;
import com.scg.domain.Consultant;
import com.scg.domain.TimeCard;
import com.scg.persistent.DbServer;
import com.scg.util.ListFactory;

/**
 * The initialize/populate the database.
 * 
 * @author chq-alexs
 *
 */
public final class InitDb {

	/**
	 * Database URL
	 */
	private final static String db = "jdbc:derby://localhost:1527/memory:scgDb";
	/**
	 * Database username
	 */
	private final static String user = "student";
	/**
	 * Database password
	 */
	private final static String pass = "student";

	/**
	 * Database dbServer object
	 */
	private static DbServer dbServer = new DbServer(db, user, pass);

	/**
	 * Entry point.
	 * 
	 * @param args
	 *            - not used.
	 * 
	 * @exception Exception
	 *                - if anything goes awry
	 * 
	 */
	public static void main(String[] args) throws Exception {
		final List<ClientAccount> accounts = new ArrayList<ClientAccount>();
		final List<Consultant> consultants = new ArrayList<Consultant>();
		final List<TimeCard> timeCards = new ArrayList<TimeCard>();
		ListFactory.populateLists(accounts, consultants, timeCards);

		accounts.forEach(items -> {
			try {
				dbServer.addClient(items);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		consultants.forEach(items -> {
			try {
				dbServer.addConsultant(items);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		timeCards.forEach(items -> {
			try {
				dbServer.addTimeCard(items);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		});

		System.out.println("Finished Loading");

	}

}
