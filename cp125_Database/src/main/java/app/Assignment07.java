package app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.scg.domain.ClientAccount;
import com.scg.domain.Invoice;
import com.scg.persistent.DbServer;

/**
 * Creates an invoice from the data in the database.
 * 
 * @author chq-alexs
 *
 */
public final class Assignment07 {

	/**
	 * Database URL
	 */

	private final static String db = "jdbc:derby://localhost:1527/memory:scgDb";

	/**
	 * Database Username
	 */
	private final static String user = "student";

	/**
	 * Database Password
	 */
	private final static String pass = "student";

	/**
	 * Database Server instance
	 */
	private static DbServer dbServer = new DbServer(db, user, pass);

	/** This class' logger. */
	private static final Logger log = LoggerFactory.getLogger(Assignment07.class);

	/** The invoice month. */
	private static final Month INVOICE_MONTH = Month.MARCH;

	/** The test year. */
	private static final int INVOICE_YEAR = 2006;

	/**
	 * Prevent instantiation.
	 */
	private Assignment07() {
	}

	/**
	 * Print the invoice to a PrintStream.
	 *
	 * @param invoices
	 *            the invoices to print
	 * @param out
	 *            The output stream; can be System.out or a text file.
	 */
	private static void printInvoices(final List<Invoice> invoices, final PrintStream out) {
		for (final Invoice invoice : invoices) {
			out.println(invoice.toReportString());
		}
	}

	/**
	 * Create invoices for the clients from the timecards.
	 *
	 * @param accounts
	 *            the accounts to create the invoices for
	 * @param timeCards
	 *            the time cards to create the invoices from
	 *
	 * @return the created invoices
	 */
	private static List<Invoice> createInvoices(final List<ClientAccount> accounts) {

		final List<Invoice> invoices = new ArrayList<Invoice>(accounts.size());
		for (final ClientAccount account : accounts) {

			Invoice invoice = null;
			try {
				invoice = dbServer.getInvoice(account, INVOICE_MONTH, INVOICE_YEAR);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			invoices.add(invoice);

		}

		return invoices;
	}

	/**
	 * Confirm the invoice totals.
	 * 
	 * @param exHours
	 *            the expected hours
	 * @param exCharges
	 *            the expected charges
	 * @param invoice
	 *            the invoice
	 */
	private static void confirmTotals(final int exHours, final int exCharges, final Invoice invoice) {
		final int invHours = invoice.getTotalHours();
		final int invCharges = invoice.getTotalCharges();
		if (invHours != exHours) {
			log.error(String.format("Invoice hours for %s are incorrect, expected %d but was %d",
					invoice.getClientAccount().getName(), exHours, invHours));
		}
		if (invCharges != exCharges) {
			log.error(String.format("Invoice charges for %s are incorrect, expected %d but was %d",
					invoice.getClientAccount().getName(), exCharges, invCharges));
		}
	}

	/**
	 * Entry point.
	 * 
	 * @param args
	 *            - not used.
	 * 
	 * @throws Exception
	 *             - if anything goes awry
	 * 
	 */
	public static void main(String[] args) throws Exception {
		List<ClientAccount> accounts = new ArrayList<ClientAccount>();

		accounts = dbServer.getClients();

		final List<Invoice> invoices = createInvoices(accounts);

		// Print them
		System.out.println();
		System.out.println("==================================================================================");
		System.out.println("=============================== I N V O I C E S ==================================");
		System.out.println("==================================================================================");
		System.out.println();
		Invoice invoice = invoices.get(0);
		confirmTotals(16, 2400, invoice);
		System.out.println(invoice.toReportString());
		invoice = invoices.get(1);
		confirmTotals(108, 19400, invoice);
		System.out.println(invoice.toReportString());

		// Now print it to a file
		PrintStream writer;
		try {
			writer = new PrintStream(new FileOutputStream("invoices.txt"));
			printInvoices(invoices, writer);
		} catch (final IOException ex) {
			log.error("Unable to print invoice.", ex);
		}
	}

}
