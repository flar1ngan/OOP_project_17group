package com.example.collaboraboard.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.collaboraboard.activities.CardDetailsActivity
import com.example.collaboraboard.activities.CreateBoardActivity
import com.example.collaboraboard.activities.MainActivity
import com.example.collaboraboard.activities.MembersActivity
import com.example.collaboraboard.activities.MyProfileActivity
import com.example.collaboraboard.activities.SignInActivity
import com.example.collaboraboard.activities.SignUpActivity
import com.example.collaboraboard.activities.TaskListActivity
import com.example.collaboraboard.models.Board
import com.example.collaboraboard.models.User
import com.example.collaboraboard.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {

    private val mFireStore = FirebaseFirestore.getInstance() //A global variable of database access

    //Function to save user data in Firebase
    fun registerUser(activity: SignUpActivity, userInfo: User){
        mFireStore.collection(Constants.USERS)      //users collection
            .document(getCurrentUserId())       //current users document
            .set(userInfo, SetOptions.merge())  //write data to Firestore Firebase with merge parameter
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener { e->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error writing document", e)
            }
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String){
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentID = document.id
                activity.boardDetails(board)
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while getting board details.", e)
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board){
        val documentReference = mFireStore.collection(Constants.BOARDS).document()
        board.documentID = documentReference.id
        documentReference.set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "Board created successfully!")
                Toast.makeText(
                    activity,
                    "Board created successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                activity.boardCreatedSuccessfully()
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error creating a board", e)
                Toast.makeText(
                    activity,
                    "Error creating a board",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun getBoardsList(activity: MainActivity){
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardList: ArrayList<Board> = ArrayList()
                for(i in document.documents){
                    val board = i.toObject(Board::class.java)!!
                    board.documentID = i.id
                    boardList.add(board)
                }
                activity.populateBoardListToUI(boardList)
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board.", e)
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board){
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentID)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "TaskList updated successfully!")
                if (activity is TaskListActivity){
                    activity.addUpdateTaskListSuccess()
                }else if (activity is CardDetailsActivity){
                    activity.addUpdateTaskListSuccess()
                }
            }.addOnFailureListener { exception ->
                if (activity is TaskListActivity){
                    activity.hideProgressDialog()
                }else if (activity is CardDetailsActivity){
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName, "TaskList update error.", exception)
            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName, "Profile data updated successfully!")
                Toast.makeText(
                    activity,
                    "Profile data updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                when(activity){
                    is MainActivity ->{
                        activity.tokenUpdateSuccess()
                    }
                    is MyProfileActivity ->{
                        activity.profileUpdateSuccess()
                    }
                }
            }.addOnFailureListener { exception ->
                when(activity){
                    is MainActivity ->{
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity ->{
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Profile data updating error!", exception)
                Toast.makeText(
                    activity,
                    "Error when updating profile!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).get()
            .addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)!!
                when(activity){
                    is SignUpActivity ->{
                        activity.autoSignInSuccess()
                    }
                    is SignInActivity ->{
                        activity.signInSuccess()
                    }
                    is MainActivity ->{
                        activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                    }
                    is MyProfileActivity ->{
                        activity.setUserDataInUI(loggedInUser)
                    }
                }
            }.addOnFailureListener {
                    e->
                when(activity){
                    is SignUpActivity ->{
                        activity.hideProgressDialog()
                    }
                    is SignInActivity ->{
                        activity.hideProgressDialog()
                    }
                    is MainActivity ->{
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity ->{
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Error while getting loggedIn user details", e)
            }
    }
    fun getCurrentUserId(): String{
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID =""
        if(currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>){
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo).get()
            .addOnSuccessListener { document ->
                Log.e(activity.javaClass.simpleName, document.documents.toString())
                val usersList: ArrayList<User> = ArrayList()
                for (i in document.documents){
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if(activity is MembersActivity){
                    activity.setupMembersList(usersList)
                }else if(activity is TaskListActivity){
                    activity.boardMembersDetailsList(usersList)
                }
            }.addOnFailureListener { e ->
                if(activity is MembersActivity){
                    activity.hideProgressDialog()
                }else if(activity is TaskListActivity){
                    activity.hideProgressDialog()
                }
                Log.e(activity.javaClass.simpleName, "Error while getting users list.", e)
            }
    }

    fun getMemberDetails(activity: MembersActivity, email: String){
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email).get()
            .addOnSuccessListener { document ->
                if(document.documents.size > 0){
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                }else{
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No user found with specific email")
                }

            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while searching user via email.", e)
            }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User){
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentID)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener{ e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while searching user via email.", e)
            }
    }
}