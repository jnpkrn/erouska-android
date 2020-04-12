package cz.covid19cz.erouska.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import cz.covid19cz.erouska.screenObject.*
import cz.covid19cz.erouska.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ActivationTest {
    private val welcomeScreen = WelcomeScreen()
    private val bluetoothPermisionScreen = BluetoothPermissionScreen()
    private val phoneNumberScreen = PhoneNumberScreen()
    private val smsScreen = SMSScreen()
    private val finishActivation = FinishActivation()

    @get:Rule
    val activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Test
    fun activation() {
        welcomeScreen.continueToActivation()
        bluetoothPermisionScreen.allowPermission()
        phoneNumberScreen.run{
            typePhoneNumber()
            acceptWithAgreements()
            continueToSMSVerify()
        }
        smsScreen.typeSMSCode()
        smsScreen.verifySMSCode()
        finishActivation.finish()
    }


}