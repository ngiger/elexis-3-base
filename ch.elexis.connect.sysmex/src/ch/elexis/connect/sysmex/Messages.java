/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package ch.elexis.connect.sysmex;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "ch.elexis.connect.sysmex.messages";
    public static String Preferences_Backgroundprocess;
    public static String Preferences_Baud;
    public static String Preferences_Databits;
    public static String Preferences_Log;
    public static String Preferences_Modell;
    public static String Preferences_Parity;
    public static String Preferences_Port;
    public static String Preferences_RDW;
    public static String Preferences_Stopbits;
    public static String Preferences_Timeout;
    public static String Preferences_Verbindung;
    public static String Probe_ResultatMsg;
    public static String SysmexAction_ButtonName;
    public static String SysmexAction_ConnectionName;
    public static String SysmexAction_DefaultParams;
    public static String SysmexAction_DefaultPort;
    public static String SysmexAction_DefaultTimeout;
    public static String SysmexAction_DeviceName;
    public static String SysmexAction_ErrorTitle;
    public static String SysmexAction_LogError_Text;
    public static String SysmexAction_LogError_Title;
    public static String SysmexAction_NoPatientMsg;
    public static String SysmexAction_PatientHeaderString;
    public static String SysmexAction_Patient_Text;
    public static String SysmexAction_Patient_Title;
    public static String SysmexAction_ProbeError_Title;
    public static String SysmexAction_RS232_Break_Text;
    public static String SysmexAction_RS232_Break_Title;
    public static String SysmexAction_RS232_Error_Text;
    public static String SysmexAction_RS232_Error_Title;
    public static String SysmexAction_RS232_Timeout_Text;
    public static String SysmexAction_RS232_Timeout_Title;
    public static String SysmexAction_ResendMsg;
    public static String SysmexAction_ToolTip;
    public static String SysmexAction_WaitMsg;
    public static String SysmexAction_WrongDataFormat;
    public static String Value_Error;
    public static String Value_High;
    public static String Value_LabKuerzel;
    public static String Value_LabName;
    public static String Value_Low;

    static { // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}
