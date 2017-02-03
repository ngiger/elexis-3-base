/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved.   This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package ch.medshare.connect.abacusjunior;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "ch.medshare.connect.abacusjunior.messages";
    public static String AbacusJuniorAction_ButtonName;
    public static String AbacusJuniorAction_ConnectionName;
    public static String AbacusJuniorAction_DefaultParams;
    public static String AbacusJuniorAction_DefaultPort;
    public static String AbacusJuniorAction_LogError_Text;
    public static String AbacusJuniorAction_LogError_Title;
    public static String AbacusJuniorAction_Patient_Text;
    public static String AbacusJuniorAction_Patient_Title;
    public static String AbacusJuniorAction_RS232_Break_Text;
    public static String AbacusJuniorAction_RS232_Break_Title;
    public static String AbacusJuniorAction_RS232_Error_Text;
    public static String AbacusJuniorAction_RS232_Error_Title;
    public static String AbacusJuniorAction_RS232_Timeout_Text;
    public static String AbacusJuniorAction_RS232_Timeout_Title;
    public static String AbacusJuniorAction_ToolTip;
    public static String Preferences_Baud;
    public static String Preferences_Databits;
    public static String Preferences_Log;
    public static String Preferences_Parity;
    public static String Preferences_Port;
    public static String Preferences_Stopbits;
    public static String Value_Error;
    public static String Value_High;
    public static String Value_LabKuerzel;
    public static String Value_LabName;
    public static String Value_Low;

    static { // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}
