package org.iatrix;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.iatrix.messages"; //$NON-NLS-1$
	public static  String FixMediDisplay_DeleteUnrecoverable;
	public static String FixMediDisplay_Change;
	public static String FixMediDisplay_Stop;
	public static String FixMediDisplay_Modify;
	public static String FixMediDisplay_StopThisMedicament;
	public static String ProblemFixMediDisplay_AlertNoProblemSelectedText;
	public static String ProblemFixMediDisplay_AlertNoProblemSelectedTitle;
	public static String FixMediDisplay_FixMedikation;
	public static String FixMediDisplay_DailyCost;
	public static String FixMediDisplay_Prescription;
	public static String FixMediDisplay_UsageList;
	public static String FixMediDisplay_AddItem;
	public static String FixMediDisplay_Copy;
	public static String JournalView_export_to_clipboard;
	public static String JournalView_export_extract_to_clipboard;
	public static String JournalView_send_email;
	public static String JournalView_open_mail_program_with_drugs;
	public static String JournalView_show_more_consultations;
	public static String JournalView_show_more_consultations_tooltip;
	public static String JournalView_show_all_consultations;
	public static String JournalView_show_all_consultations_tooltip;
	public static String KonsListView_show_all_consultations;
	public static String KonsListView_show_all_consultations_tooltip;
	public static String KonsListView_show_more_consultations;
	public static String KonsListView_show_more_consultations_tooltip;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages(){}
}
