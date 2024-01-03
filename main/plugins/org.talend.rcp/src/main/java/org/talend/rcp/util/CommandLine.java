// ============================================================================
//
// Copyright (C) 2022-2023 Talaxie Inc. - www.deilink.fr
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talaxie SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.rcp.util;

import java.util.Scanner;
import org.talend.repository.ui.wizards.exportjob.JobScriptsExportWizardPage;
import org.talend.rcp.util.ServerRest;

public class CommandLine {

  public static boolean parseArgs(String[] args) {
    if (args == null) {
      return false;
    }

    // Traiter les arguments
    System.out.println("Start Talaxie...");
    String argString = "";
    for (String arg : args) {
      if (
          arg.length() > 4 &&
          arg.contains("--") &&
          arg.contains("=")
      ) {
        String[] splitFromEqual = arg.split("=");
        String key = splitFromEqual[0].substring(2);
        String value = splitFromEqual[1];
        argString += key + "/" + value + " [" + arg + "] || ";
        if (key.equals("offline")) {
          System.out.print("Enter a string 2 : ");
          Scanner scanner2 = new Scanner(System.in);
          String inputString2 = scanner2.nextLine();
          System.out.println("Offline input zone : \n" + inputString2);
          // throw new Exception("Exception talaxie JC 200 : " + argString);
          return true;
        } else if (key.equals("api")) {
          try {
            ServerRest.startServer(value);
            System.out.print("End REST API ?");
            Scanner scanner = new Scanner(System.in);
            String inputString = scanner.nextLine();
            return true;
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }

    return false;
  }
}
