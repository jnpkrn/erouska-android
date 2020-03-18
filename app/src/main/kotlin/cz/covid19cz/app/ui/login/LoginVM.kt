package cz.covid19cz.app.ui.login

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import cz.covid19cz.app.db.ExpositionRepository
import cz.covid19cz.app.ui.base.BaseVM
import java.util.*

class LoginVM(val deviceRepository: ExpositionRepository) : BaseVM() {

    val data = deviceRepository.data
    val state = MutableLiveData<LoginState>(EnterPhoneNumber)
    val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)
            state.postValue(LoginError(e))
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            this@LoginVM.verificationId = verificationId
            resendToken = token
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            Log.d(TAG, "onCodeAutoRetrievalTimeOut:$verificationId")
            this@LoginVM.verificationId = verificationId
            state.postValue(EnterCode)
        }
    }
    private val TAG = "Login"
    private lateinit var verificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    init {
        auth.setLanguageCode("cs")
        if (auth.currentUser != null) {
            getUser()
        }
    }

    fun codeEntered(code: String) {
        state.postValue(SigningProgress)
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    writeUserToFirestore()
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    state.postValue(LoginError(checkNotNull(task.exception)))
                    //if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    //}
                }
            }
    }

    private fun writeUserToFirestore() {
        val uid = checkNotNull(auth.uid)
        db.collection("users").document(uid).get().addOnSuccessListener {
            if (it.exists()) {
                val buid = it.data?.get("buid") as String
                updateUser(uid, buid)
            } else {
                // TODO: generate BUID using backend
                val buid = UUID.randomUUID().toString().takeLast(10)
                updateUser(uid, buid)
            }
        }
    }

    private fun updateUser(uid: String, buid: String) {
        val phoneNumber = checkNotNull(auth.currentUser?.phoneNumber)
        db.collection("users").document(uid).set(UserModel(buid, phoneNumber))
            .addOnSuccessListener {
                state.postValue(SignedIn(uid, phoneNumber, buid))
            }
    }

    private fun getUser() {
        val uid = checkNotNull(auth.uid)
        val phoneNumber = checkNotNull(auth.currentUser?.phoneNumber)
        db.collection("users").document(uid).get().addOnSuccessListener {
            if (it.exists()) {
                // Document exists
                val buid = it.data?.get("buid") as String
                state.postValue(SignedIn(uid, phoneNumber, buid))
            }
        }
    }

}