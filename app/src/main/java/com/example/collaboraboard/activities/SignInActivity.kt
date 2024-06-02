package com.example.collaboraboard.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.example.collaboraboard.R
import com.example.collaboraboard.databinding.ActivitySignInBinding
import com.example.collaboraboard.firebase.FirestoreClass
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : BaseActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_sign_in)
        setContentView(binding.root)

        //Initialize the Firebase variable
        auth = FirebaseAuth.getInstance()

        //Hides the status bar on different android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        binding.btnSignIn.setOnClickListener {
            signInRegisteredUser()
        }

        setupActionBar()
    }

    //Set the button to go back to the Intro screen
    private fun setupActionBar(){
        setSupportActionBar(binding.toolbarSignInActivity)
        val actionBar = supportActionBar
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
        }
        binding.toolbarSignInActivity.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun signInRegisteredUser(){
        val email: String = binding.etEmailSignIn.text.toString().trim { it <= ' ' }
        val password: String = binding.etPasswordSignIn.text.toString().trim { it <= ' ' }

        if(validateForm(email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    hideProgressDialog()
                    if (task.isSuccessful) {
                        // Sign in success
                        Log.d("SignIn", "signInWithEmail:success")
                        FirestoreClass().loadUserData(this)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("SignIn", "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this@SignInActivity,
                            task.exception!!.message,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }

    private fun validateForm(email: String, password: String) : Boolean{
        return when{
            TextUtils.isEmpty(email)->{
                showErrorSnackBar("Please enter an email address")
                false
            }
            TextUtils.isEmpty(password)->{
                showErrorSnackBar("Please enter a password")
                false
            } else ->{
                true
            }
        }
    }

    fun signInSuccess(){
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

}