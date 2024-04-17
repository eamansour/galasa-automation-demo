/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.simbank.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;

import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.Test;
import dev.galasa.artifact.BundleResources;
import dev.galasa.artifact.IBundleResources;
import dev.galasa.core.manager.CoreManager;
import dev.galasa.core.manager.ICoreManager;
import dev.galasa.http.HttpClient;
import dev.galasa.http.IHttpClient;
import dev.galasa.simbank.manager.ISimBank;
import dev.galasa.simbank.manager.SimBank;
import dev.galasa.zos.IZosImage;
import dev.galasa.zos.ZosImage;
import dev.galasa.zos3270.ITerminal;
import dev.galasa.zos3270.Zos3270Terminal;

@Test
public class BasicAccountCreditTest {

    @SimBank
    public ISimBank simBank;

    @ZosImage(imageTag = "SIMBANK")
    public IZosImage image;

    @Zos3270Terminal(imageTag = "SIMBANK")
    public ITerminal terminal;

    @BundleResources
    public IBundleResources resources;

    @CoreManager
    public ICoreManager coreManager;

    @HttpClient
    public IHttpClient client;

    private static final String CREDENTIALS_ID = "MYSIMBANKUSER";

    /**
     * Test which checks the initial balance of an account, uses the webservice to
     * credit the account, then checks the balance again. The test passes if the
     * final balance is equal to the old balance + the credited amount.
     */
    @Test
    public void updateAccountWebServiceTest() throws Exception {
        // Get the credentials that will be used to log in to SimBank
        ICredentialsUsernamePassword credentials = (ICredentialsUsernamePassword) coreManager.getCredentials(CREDENTIALS_ID);
        String password = credentials.getPassword();

        // Register the password to the confidential text filtering service
        coreManager.registerConfidentialText(password, "IBMUSER password");

        // Initial actions to get into banking application
        terminal.waitForKeyboard()
            .positionCursorToFieldContaining("Userid").tab()
            .type(credentials.getUsername())
            .positionCursorToFieldContaining("Password").tab()
            .type(password)
            .enter()
            .waitForKeyboard()

            // Open banking application
            .pf1().waitForKeyboard().clear().waitForKeyboard()
            .type("bank")
            .enter().waitForKeyboard();

        // Obtain the initial balance
        BigDecimal userBalance = getBalance("123456789");

        // Set the amount be credited and call web service
        BigDecimal amount = BigDecimal.valueOf(500.50);
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("ACCOUNT_NUMBER", "123456789");
        parameters.put("AMOUNT", amount.toString());

        // Load sample request with the given parameters
        String textContent = resources.retrieveSkeletonFileAsString("/resources/skeletons/testSkel.skel", parameters);

        // Invoke the web request
        client.setURI(new URI("http://" + this.simBank.getHost() + ":" + this.simBank.getWebnetPort()));
        client.postText("updateAccount", textContent);

        // Obtain the final balance
        BigDecimal newUserBalance = getBalance("123456789");

        // Assert that the correct amount has been credited to the account
        assertThat(newUserBalance).isEqualTo(userBalance.add(amount));
    }

    /**
     * Navigate through the banking application and extract the balance of a given
     * account
     *
     * @param accountNum - Account Number of the account being queried
     * @return Balance of the account being queried
     */
    private BigDecimal getBalance(String accountNum)
            throws Exception {
        BigDecimal amount = BigDecimal.ZERO;
        // Open account menu and enter account number
        terminal.pf1().waitForKeyboard().positionCursorToFieldContaining("Account Number").tab().type(accountNum)
                .enter().waitForKeyboard();

        // Retrieve balance from screen
        amount = new BigDecimal(terminal.retrieveFieldTextAfterFieldWithString("Balance").trim());

        // Return to bank menu
        terminal.pf3().waitForKeyboard();
        return amount;
    }
}
