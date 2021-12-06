package com.gtech.kotlinadhar

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.LinearLayout
import android.os.Bundle
import com.gtech.kotlinadhar.R
import android.content.pm.PackageManager
import com.google.zxing.integration.android.IntentIntegrator
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gtech.kotlinadhar.utils.*
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.json.JSONObject
import org.json.JSONArray
import com.kinda.alert.KAlertDialog
import com.gtech.kotlinadhar.utils.*
import org.json.JSONException
import java.io.IOException
import java.io.StringReader
import java.util.regex.Matcher
import java.util.regex.Pattern

class HomeActivity : AppCompatActivity() {
    var uid: String? = null
    var name: String? = null
    var gender: String? = null
    var yearOfBirth: String? = null
    var careOf: String? = null
    var villageTehsil: String? = null
    var postOffice: String? = null
    var district: String? = null
    var state: String? = null
    var postCode: String? = null
    var aadharData: AadharCard? = null

    // UI Elements
    var tv_sd_uid: TextView? = null
    var tv_sd_name: TextView? = null
    var tv_sd_gender: TextView? = null
    var tv_sd_yob: TextView? = null
    var tv_sd_co: TextView? = null
    var tv_sd_vtc: TextView? = null
    var tv_sd_po: TextView? = null
    var tv_sd_dist: TextView? = null
    var tv_sd_state: TextView? = null
    var tv_sd_pc: TextView? = null
    var tv_cancel_action: TextView? = null
    var ll_scanned_data_wrapper: LinearLayout? = null
    var ll_data_wrapper: LinearLayout? = null
    var ll_action_button_wrapper: LinearLayout? = null

    // Storage
    var storage: Storage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //hide the default action bar
//        supportActionBar!!.hide()
        setContentView(R.layout.activity_home)

        // init the UI Elements
        tv_sd_uid = findViewById<View>(R.id.tv_sd_uid) as TextView
        tv_sd_name = findViewById<View>(R.id.tv_sd_name) as TextView
        tv_sd_gender = findViewById<View>(R.id.tv_sd_gender) as TextView
        tv_sd_yob = findViewById<View>(R.id.tv_sd_yob) as TextView
        tv_sd_co = findViewById<View>(R.id.tv_sd_co) as TextView
        tv_sd_vtc = findViewById<View>(R.id.tv_sd_vtc) as TextView
        tv_sd_po = findViewById<View>(R.id.tv_sd_po) as TextView
        tv_sd_dist = findViewById<View>(R.id.tv_sd_dist) as TextView
        tv_sd_state = findViewById<View>(R.id.tv_sd_state) as TextView
        tv_sd_pc = findViewById<View>(R.id.tv_sd_pc) as TextView
        tv_cancel_action = findViewById<View>(R.id.tv_cancel_action) as TextView
        ll_scanned_data_wrapper = findViewById<View>(R.id.ll_scanned_data_wrapper) as LinearLayout
        ll_data_wrapper = findViewById<View>(R.id.ll_data_wrapper) as LinearLayout
        ll_action_button_wrapper = findViewById<View>(R.id.ll_action_button_wrapper) as LinearLayout

        //init storage
        storage = Storage(this)
    }

    /**
     * Function to check if user has granted access to camera
     * @return boolean
     */
    fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                MY_CAMERA_REQUEST_CODE
            )
            return false
        }
        return true
    }

    /**
     * onclick handler for scan new card
     * @param view
     */
    fun scanNow(view: View?) {
        // we need to check if the user has granted the camera permissions
        // otherwise scanner will not work
        if (!checkCameraPermission()) {
            return
        }
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
        integrator.setPrompt("Scan a Aadharcard QR Code")
        integrator.setResultDisplayDuration(500)
        integrator.setCameraId(0) // Use a specific camera of the device
        integrator.initiateScan()
    }

    /**
     * function handle scan result
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        //retrieve scan result
        val scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
        if (scanningResult != null) {
            //we have a result
            var scanContent = scanningResult.contents
            val scanFormat = scanningResult.formatName
            Log.d(TAG, " contents ${scanningResult.contents}")
            Log.d(TAG, " errorcorrectionlevel ${scanningResult.errorCorrectionLevel}")
            Log.d(TAG, "formant name: ${scanningResult.formatName}")
            Log.d(TAG, " orientation: ${scanningResult.orientation}")
            Log.d(TAG, "onActivityResult rawbytes: ${scanningResult.rawBytes}")

            // process received data
            if (scanContent != null && !scanContent.isEmpty()) {
                Log.d(TAG, "onActivityResult: contents ${scanningResult.contents}")
                Log.d(TAG, "onActivityResult: errorcorrectionlevel ${scanningResult.errorCorrectionLevel}")
                Log.d(TAG, "onActivityResult:formant name: ${scanningResult.formatName}")
                Log.d(TAG, "onActivityResult orientation: ${scanningResult.orientation}")
                Log.d(TAG, "onActivityResult rawbytes: ${scanningResult.rawBytes}")
                // Replace </?xml... with <?xml...
                if (scanContent.startsWith("</?")) {
                    Log.d(TAG, "onActivityResult: Starts with </?")
                    scanContent = scanContent.replaceFirst("</\\?", "<?");
                }
                // Replace <?xml...?"> with <?xml..."?>
                scanContent = scanContent.replaceFirst("^<\\?xml ([^>]+)\\?\">", "<?xml $1\"?>");
                Log.d(TAG, "isxml : ${isXml(scanContent)}")


try{
    processScannedData(scanContent)
}
catch (e:Exception){
    Log.e(Companion.TAG,e.message+"")
}
            } else {
                showWarningPrompt("Scan Cancelled")
            }
        } else {
            showWarningPrompt("No scan data received!")
        }
    }

    /**
     * process encoded string received from aadhaar card QR code
     * @param scanData
     */
    protected fun processScannedData(scanData: String?) {
        // check if the scanned string is XML
        // This is to support old QR codes

        if (!isXml(scanData)) {
            val pullParserFactory: XmlPullParserFactory
            try {
                // init the parserfactory
                pullParserFactory = XmlPullParserFactory.newInstance()
                // get the parser
                val parser = pullParserFactory.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(StringReader(scanData))
                aadharData = AadharCard()

                // parse the XML
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        Log.d("Rajdeol", "Start document")
                    } else if (eventType == XmlPullParser.START_TAG && DataAttributes.AADHAAR_DATA_TAG == parser.name) {
                        // extract data from tag
// village Tehsil
                        aadharData!!.vtc = parser.getAttributeValue(null, DataAttributes.AADHAR_VTC_ATTR)

                            //uid
                        aadharData!!.uuid = parser.getAttributeValue(null, DataAttributes.AADHAR_UID_ATTR)


                        //name
                        aadharData!!.name = parser.getAttributeValue(null, DataAttributes.AADHAR_NAME_ATTR)
                        //gender
                        aadharData!!.gender =
                            parser.getAttributeValue(null, DataAttributes.AADHAR_GENDER_ATTR)

                        // year of birth
                        parser.getAttributeValue(null, DataAttributes.AADHAR_DOB_ATTR)?.let {     aadharData!!.dateOfBirth = it}
//                         care of
                        parser.getAttributeValue(null, DataAttributes.AADHAR_CO_ATTR)?.let{  aadharData!!.careOf = it}

                        // Post Office
//                        aadharData!!.postOffice = parser.getAttributeValue(null, DataAttributes.AADHAR_PO_ATTR)
                        // district
                        aadharData!!.district =
                            parser.getAttributeValue(null, DataAttributes.AADHAR_DIST_ATTR)
                        // state
                        aadharData!!.state =
                            parser.getAttributeValue(null, DataAttributes.AADHAR_STATE_ATTR)
                        // Post Code
//                        aadharData!!.pinCode = parser.getAttributeValue(null, DataAttributes.AADHAR_PC_ATTR)


                    //    Toast.makeText(this, aadharData!!.uuid, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "processScannedData Name: ${aadharData!!.name}")
                        Log.d(TAG, "processScannedData PIN: ${aadharData!!.pinCode}")
                        Log.d(TAG, "processScannedData VTC: ${aadharData!!.vtc}")
                        Log.d(TAG, "processScannedData UUID: ${aadharData!!.uuid}")
                    } else if (eventType == XmlPullParser.END_TAG) {
                        Log.d("Rajdeol", "End tag " + parser.name)
                    } else if (eventType == XmlPullParser.TEXT) {
                        Log.d("Rajdeol", "Text " + parser.text)
                    }
                    // update eventType
                    eventType = parser.next()
                }

                // display the data on screen
                displayScannedData()
                return
            } catch (e: XmlPullParserException) {
                showErrorPrompt("Error in processing QRcode XML")
                e.printStackTrace()
                return
            } catch (e: IOException) {
                showErrorPrompt(e.toString())
                e.printStackTrace()
                return
            }
        }

        // process secure QR code
//      else  processEncodedScannedData(scanData)
    } // EO function

    /**
     * Function to process encoded aadhar data
     * @param scanData
     */
    protected fun processEncodedScannedData(scanData: String?) {
        try {
            val decodedData = SecureQrCode(this, scanData)
            aadharData = decodedData.scannedAadharCard
            // display the Aadhar Data
            showSuccessPrompt("Scanned Aadhar Card Successfully")
            displayScannedData()
        } catch (e: QrCodeException) {
            showErrorPrompt(e.toString())
            e.printStackTrace()
        }
    }

    /**
     * show scanned information
     */
    fun displayScannedData() {
        ll_data_wrapper!!.visibility = View.GONE
        ll_scanned_data_wrapper!!.visibility = View.VISIBLE
        ll_action_button_wrapper!!.visibility = View.VISIBLE

        // clear old values if any
        tv_sd_uid!!.text = ""
        tv_sd_name!!.text = ""
        tv_sd_gender!!.text = ""
        tv_sd_yob!!.text = ""
        tv_sd_co!!.text = ""
        tv_sd_vtc!!.text = ""
        tv_sd_po!!.text = ""
        tv_sd_dist!!.text = ""
        tv_sd_state!!.text = ""
        tv_sd_pc!!.text = ""

        // update UI Elements
        tv_sd_uid!!.text = aadharData!!.uuid
        tv_sd_name!!.text = aadharData!!.name
        tv_sd_gender!!.text = aadharData!!.gender
        tv_sd_yob!!.text = aadharData!!.dateOfBirth
        tv_sd_co!!.text = aadharData!!.careOf
        tv_sd_vtc!!.text = aadharData!!.vtc
        tv_sd_po!!.text = aadharData!!.postOffice
        tv_sd_dist!!.text = aadharData!!.district
        tv_sd_state!!.text = aadharData!!.state
        tv_sd_pc!!.text = aadharData!!.pinCode
    }

    /**
     * display home screen onclick listener for cancel button
     * @param view
     */
    fun showHome(view: View?) {
        ll_data_wrapper!!.visibility = View.VISIBLE
        ll_scanned_data_wrapper!!.visibility = View.GONE
        ll_action_button_wrapper!!.visibility = View.GONE
    }

    /**
     * save data to storage
     */
    fun saveData(view: View?) {
        // We are going to use json to save our data
        // create json object
        val aadharDataJson = JSONObject()
        try {
            aadharDataJson.put(DataAttributes.AADHAR_UID_ATTR, aadharData!!.uuid)
            aadharDataJson.put(DataAttributes.AADHAR_NAME_ATTR, aadharData!!.name)
            aadharDataJson.put(DataAttributes.AADHAR_GENDER_ATTR, aadharData!!.gender)
//            DataAttributes.AADHAR_DOB_ATTR.let{     aadharDataJson.put(DataAttributes.AADHAR_DOB_ATTR, aadharData!!.dateOfBirth)}
            aadharDataJson.put(DataAttributes.AADHAR_CO_ATTR, aadharData!!.careOf)
            aadharDataJson.put(DataAttributes.AADHAR_VTC_ATTR, aadharData!!.vtc)
            aadharDataJson.put(DataAttributes.AADHAR_PO_ATTR, aadharData!!.postOffice)
            aadharDataJson.put(DataAttributes.AADHAR_DIST_ATTR, aadharData!!.district)
            aadharDataJson.put(DataAttributes.AADHAR_STATE_ATTR, aadharData!!.state)
            aadharDataJson.put(DataAttributes.AADHAR_PC_ATTR, aadharData!!.pinCode)
            aadharDataJson.put(DataAttributes.AADHAR_LAND_ATTR, aadharData!!.landmark)
            aadharDataJson.put(DataAttributes.AADHAR_HOUSE_ATTR, aadharData!!.house)
            aadharDataJson.put(DataAttributes.AADHAR_LOCATION_ATTR, aadharData!!.location)
            aadharDataJson.put(DataAttributes.AADHAR_STREET_ATTR, aadharData!!.street)
            aadharDataJson.put(DataAttributes.AADHAR_SUBDIST_ATTR, aadharData!!.subDistrict)
            aadharDataJson.put(DataAttributes.AADHAR_EMAIL_ATTR, aadharData!!.email)
            aadharDataJson.put(DataAttributes.AADHAR_MOBILE_ATTR, aadharData!!.mobile)
            aadharDataJson.put(DataAttributes.AADHAR_SIG_ATTR, aadharData!!.signature)

            // read data from storage
            val storageData = storage!!.readFromFile()
            val storageDataArray: JSONArray
            //check if file is empty
            storageDataArray = if (storageData.length > 0) {
                JSONArray(storageData)
            } else {
                JSONArray()
            }


            // check if storage is empty
            if (storageDataArray.length() > 0) {
                // check if data already exists
                for (i in 0 until storageDataArray.length()) {
                    val dataUid =
                        storageDataArray.getJSONObject(i).getString(DataAttributes.AADHAR_UID_ATTR)
                    if (uid == dataUid) {
                        // do not save anything and go back
                        // show home screen
                        tv_cancel_action!!.performClick()
                        return
                    }
                }
            }
            // add the aadhaar data
            storageDataArray.put(aadharDataJson)
            // save the aadhaardata
            storage!!.writeToFile(storageDataArray.toString())

            // show home screen
            tv_cancel_action!!.performClick()
        } catch (e: JSONException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    /**
     * Function to check if string is xml
     * @param testString
     * @return boolean
     */
    protected fun isXml(testString: String?): Boolean {
        val pattern: Pattern
        val matcher: Matcher
        var retBool = false

        // REGULAR EXPRESSION TO SEE IF IT AT LEAST STARTS AND ENDS
        // WITH THE SAME ELEMENT
        val XML_PATTERN_STR = "<(\\S+?)(.*?)>(.*?)</\\1>"

        // IF WE HAVE A STRING
        if (testString != null && testString.trim { it <= ' ' }.isNotEmpty()) {

            // IF WE EVEN RESEMBLE XML
            if (testString.trim { it <= ' ' }.startsWith("<")) {
                pattern = Pattern.compile(
                    XML_PATTERN_STR,
                    Pattern.CASE_INSENSITIVE or Pattern.DOTALL or Pattern.MULTILINE
                )

                // RETURN TRUE IF IT HAS PASSED BOTH TESTS
                matcher = pattern.matcher(testString)
                retBool = matcher.matches()
            }
            // ELSE WE ARE FALSE
        }
        return retBool
    }


    fun showSavedCards(view: View?) {
        // intent for SavedAadhaarcardActivity
        val intent = Intent(this, SavedAadhaarCardActivity::class.java)
        // Start Activity
        startActivity(intent)
    }

    fun showErrorPrompt(message: String?) {
        KAlertDialog(this, KAlertDialog.ERROR_TYPE)
            .setTitleText("Error")
            .setContentText(message)
            .show()
    }

    fun showSuccessPrompt(message: String?) {
        KAlertDialog(this, KAlertDialog.SUCCESS_TYPE)
            .setTitleText("Success")
            .setContentText(message)
            .show()
    }

    fun showWarningPrompt(message: String?) {
        KAlertDialog(this, KAlertDialog.WARNING_TYPE)
            .setContentText(message)
            .show()
    }

    companion object {
        private const val MY_CAMERA_REQUEST_CODE = 100

        // variables to store extracted xml data
        private const val TAG = "HomeActivity"
    }
} // EO class
