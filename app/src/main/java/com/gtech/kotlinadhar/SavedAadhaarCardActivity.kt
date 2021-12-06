package com.gtech.kotlinadhar

import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import android.os.Bundle
import com.gtech.kotlinadhar.utils.CardListAdapter
import com.gtech.kotlinadhar.utils.DataAttributes
import android.content.Intent
import android.view.View
import android.widget.ListView
import com.gtech.kotlinadhar.utils.Storage
import org.json.JSONException
import java.util.ArrayList

class SavedAadhaarCardActivity : AppCompatActivity() {
    private var lv_saved_card_list: ListView? = null
    private var tv_no_saved_card: TextView? = null
    private var storage: Storage? = null
    private var storageDataArray: JSONArray? = null
    private var cardDataList: ArrayList<JSONObject>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        //hide the default action bar
        supportActionBar!!.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_aadhaar_card)

        //init UI elements
        tv_no_saved_card = findViewById<View>(R.id.tv_no_saved_card) as TextView
        lv_saved_card_list = findViewById<View>(R.id.lv_saved_card_list) as ListView

        // init storage
        storage = Storage(this)

        // read data from storage
        val storageData = storage!!.readFromFile()

        //check if file is not empty
        if (storageData.length > 0) {
            try {
                // convert JSON string to array
                storageDataArray = JSONArray(storageData)

                // handle case of empty JSONArray after delete
                if (storageDataArray!!.length() < 1) {
                    // hide list and show message
                    tv_no_saved_card!!.visibility = View.VISIBLE
                    lv_saved_card_list!!.visibility = View.GONE
                    //exit
                    return
                }

                // init data list
                cardDataList = ArrayList()

                //prepare the data list for list adapter
                for (i in 0 until storageDataArray!!.length()) {
                    val dataObject = storageDataArray!!.getJSONObject(i)
                    cardDataList!!.add(dataObject)
                }

                // create List Adapter with data
                val savedCardListAdapter: CardListAdapter =
                    CardListAdapter(this, cardDataList)
                // populate list
                lv_saved_card_list!!.adapter = savedCardListAdapter
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            // hide list and show message
            tv_no_saved_card!!.visibility = View.VISIBLE
            lv_saved_card_list!!.visibility = View.GONE
        }
    }

    /**
     * delete saved aadhaar card
     * @param uid
     */
    fun deleteCard(uid: String) {
        // read data from storage
        val storageData = storage!!.readFromFile()
        val storageDataArray: JSONArray
        //check if file is empty
        if (storageData.length > 0) {
            try {
                storageDataArray = JSONArray(storageData)
                // coz I am working on Android version which doesnot support remove method on JSONArray
                val updatedStorageDataArray = JSONArray()

                // check if data already exists
                for (i in 0 until storageDataArray.length()) {
                    val dataUid =
                        storageDataArray.getJSONObject(i).getString(DataAttributes.AADHAR_UID_ATTR)
                    if (uid != dataUid) {
                        updatedStorageDataArray.put(storageDataArray.getJSONObject(i))
                    }
                }

                // save the updated list
                storage!!.writeToFile(updatedStorageDataArray.toString())

                // Hide the list if all cards are deleted
                if (updatedStorageDataArray.length() < 1) {
                    // hide list and show message
                    tv_no_saved_card!!.visibility = View.VISIBLE
                    lv_saved_card_list!!.visibility = View.GONE
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start Home Activity
     * @param view
     */
    fun showHome(view: View?) {
        // intent for HomeActivity
        val intent = Intent(this, HomeActivity::class.java)
        // Start Activity
        startActivity(intent)
    }
} //EO class
