/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.Test;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.Zos3270Terminal;

@Test
public class SimBankIVT {

    @ZosImage(imageTag = "SIMBANK")
    public IZosImage image;

    @Zos3270Terminal(imageTag = "SIMBANK")
    public ITerminal terminal;

    @CoreManager
    public ICoreManager coreManager;

    private static final String CREDENTIALS_ID = "MYSIMBANKUSER";


    @Test
    public void checkBankIsAvailable() throws Exception{
        // Get the credentials that will be used to log in to SimBank
        ICredentialsUsernamePassword credentials = (ICredentialsUsernamePassword) coreManager.getCredentials(CREDENTIALS_ID);

        // Register the password to the confidential text filtering service
        String password = credentials.getPassword();
        coreManager.registerConfidentialText(password, "The password to access SimBank");

        // Logon through the session manager
        terminal.waitForKeyboard()
            .positionCursorToFieldContaining("Userid").tab()
            .type(credentials.getUsername())
            .positionCursorToFieldContaining("Password").tab()
            .type(password)
            .enter()
            .waitForKeyboard();

        // Assert that the session manager has a bank session available
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("SIMPLATFORM MAIN MENU");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BANKTEST");

        // Open banking application
        terminal.pf1().waitForKeyboard().clear().waitForKeyboard();

        terminal.type("bank").enter().waitForKeyboard();

        // Assert that the bank menu is showing
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("Options     Description        PFKey ");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("BROWSE      Browse Accounts    PF1");
        assertThat(terminal.retrieveScreen()).containsOnlyOnce("TRANSF      Transfer Money     PF4");
    }
}
